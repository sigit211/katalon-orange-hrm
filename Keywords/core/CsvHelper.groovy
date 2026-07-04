package core

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import org.apache.commons.lang3.StringUtils
import com.kms.katalon.core.util.KeywordUtil
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter

/**
 * CsvHelper
 *
 * Kumpulan utility method untuk membaca dan memanipulasi file CSV menggunakan OpenCSV.
 * Class ini hanya berisi static method — tidak boleh diinstansiasi.
 *
 * Konvensi baris:
 *   - Baris ke-0 selalu dianggap sebagai header.
 *   - Indeks baris data (untuk parameter bertipe int) dimulai dari 1 (bukan 0).
 *   - Kolom pertama (index 0) dianggap sebagai kolom ID untuk keperluan renumber.
 */
final class CsvHelper {

	private CsvHelper() {}

	// ── Read ─────────────────────────────────────────────────────────────────

	/**
	 * Membaca seluruh isi file CSV termasuk header.
	 * @param filePath  Path absolut atau relatif ke file CSV.
	 * @return List berisi setiap baris sebagai String[].
	 */
	static List<String[]> readAll(String filePath) {
		KeywordUtil.logInfo("[CsvHelper.readAll] Membaca CSV: ${filePath}")
		new CSVReader(new FileReader(filePath)).withCloseable { reader ->
			List<String[]> data = reader.readAll()
			KeywordUtil.logInfo("[CsvHelper.readAll] Total baris (termasuk header): ${data.size()}")
			return data
		}
	}

	/**
	 * Membaca seluruh isi file CSV, melewati baris header (baris pertama dilewati).
	 * @param filePath  Path absolut atau relatif ke file CSV.
	 * @return List berisi setiap baris data (tanpa header) sebagai String[].
	 */
	static List<String[]> readAllSkipHeader(String filePath) {
		KeywordUtil.logInfo("[CsvHelper.readAllSkipHeader] Membaca CSV tanpa header: ${filePath}")
		def reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)
		new CSVReaderBuilder(reader).withSkipLines(1).build().withCloseable { csvReader ->
			List<String[]> data = csvReader.readAll()
			KeywordUtil.logInfo("[CsvHelper.readAllSkipHeader] Total baris data: ${data.size()}")
			return data
		}
	}

	// ── Write ─────────────────────────────────────────────────────────────────

	/**
	 * Menulis ulang seluruh isi file CSV dengan data yang diberikan.
	 * Jika file sudah ada, isinya akan ditimpa sepenuhnya.
	 * @param filePath  Path absolut atau relatif ke file CSV tujuan.
	 * @param rows      List berisi setiap baris sebagai String[].
	 */
	static void writeAll(String filePath, List<String[]> rows) {
		KeywordUtil.logInfo("[CsvHelper.writeAll] Menulis ${rows.size()} baris ke: ${filePath}")
		new CSVWriter(new FileWriter(filePath)).withCloseable { writer ->
			writer.writeAll(rows)
			writer.flush()
		}
		KeywordUtil.logInfo("[CsvHelper.writeAll] Selesai menulis: ${filePath}")
	}

	// ── Update ────────────────────────────────────────────────────────────────

	/**
	 * Mengubah nilai satu sel berdasarkan indeks baris dan kolom, lalu menyimpan kembali ke file.
	 * @param filePath    Path absolut atau relatif ke file CSV.
	 * @param rowIndex    Indeks baris (0 = header, 1 = data pertama).
	 * @param colIndex    Indeks kolom (0-based).
	 * @param newValue    Nilai baru yang akan diisikan pada sel tersebut.
	 */
	static void updateCell(String filePath, int rowIndex, int colIndex, String newValue) {
		KeywordUtil.logInfo("[CsvHelper.updateCell] Update baris=${rowIndex}, kolom=${colIndex}, nilai='${newValue}' di: ${filePath}")
		List<String[]> allData = readAll(filePath)
		allData.get(rowIndex)[colIndex] = newValue
		writeAll(filePath, allData)
		KeywordUtil.logInfo("[CsvHelper.updateCell] Selesai update sel")
	}

	// ── Add rows ──────────────────────────────────────────────────────────────

	/**
	 * Mengatur ulang isi file CSV menjadi hanya header diikuti sejumlah baris kosong.
	 * Berguna untuk me-reset file data sebelum diisi ulang dari awal.
	 * Jumlah kolom tiap baris kosong mengikuti jumlah kolom pada header.
	 * @param filePath   Path absolut atau relatif ke file CSV.
	 * @param rowCount   Jumlah baris kosong yang ditambahkan di bawah header.
	 */
	static void resetWithEmptyRows(String filePath, int rowCount) {
		KeywordUtil.logInfo("[CsvHelper.resetWithEmptyRows] Reset file dengan ${rowCount} baris kosong: ${filePath}")

		String headerLine = readAll(filePath).get(0).toString()
				.replaceAll(/[\[\]]/, '')
				.replace(', ', ',')
		int colCount = StringUtils.countMatches(headerLine, ',') + 1

		String[] headerRow = headerLine.split(',')
		String[] emptyRow  = new String[colCount]
		Arrays.fill(emptyRow, '')

		List<String[]> rows = new ArrayList<>()
		rows.add(headerRow)
		rowCount.times { rows.add(emptyRow.clone()) }

		writeAll(filePath, rows)
		KeywordUtil.logInfo("[CsvHelper.resetWithEmptyRows] Selesai reset file")
	}

	// ── Remove ────────────────────────────────────────────────────────────────

	/**
	 * Menghapus satu baris data berdasarkan indeks, lalu melakukan renumber pada kolom pertama (ID).
	 * @param filePath       Path absolut atau relatif ke file CSV.
	 * @param dataRowIndex   Indeks baris data yang dihapus (dimulai dari 1).
	 * @throws IllegalArgumentException jika indeks di luar jangkauan data.
	 */
	static void removeRowAndRenumber(String filePath, int dataRowIndex) {
		KeywordUtil.logInfo("[CsvHelper.removeRowAndRenumber] Hapus baris ke-${dataRowIndex} dari: ${filePath}")

		List<String[]> allData = readAll(filePath)
		String[] header        = allData.get(0)
		List<String[]> dataRows = new ArrayList<>(allData.subList(1, allData.size()))

		if (dataRowIndex < 1 || dataRowIndex > dataRows.size()) {
			throw new IllegalArgumentException(
			"[CsvHelper.removeRowAndRenumber] Indeks baris tidak valid: ${dataRowIndex} (jumlah data: ${dataRows.size()})"
			)
		}

		dataRows.remove(dataRowIndex - 1)
		renumberFirstColumn(dataRows)

		List<String[]> updated = [[header], dataRows].flatten() as List<String[]>
		writeAll(filePath, updated)
		KeywordUtil.logInfo("[CsvHelper.removeRowAndRenumber] Selesai hapus baris ke-${dataRowIndex}")
	}

	/**
	 * Menghapus sejumlah baris teratas (setelah header) dari file CSV, lalu melakukan renumber.
	 * Jika removeCount >= jumlah data, file akan dikosongkan (hanya tersisa header).
	 * @param filePath     Path absolut atau relatif ke file CSV.
	 * @param removeCount  Jumlah baris teratas yang dihapus.
	 * @throws FileNotFoundException jika file tidak ditemukan.
	 * @throws IOException           jika file tidak dapat dibaca.
	 */
	static void removeTopRowsAndRenumber(String filePath, int removeCount) {
		KeywordUtil.logInfo("[CsvHelper.removeTopRowsAndRenumber] Hapus ${removeCount} baris teratas dari: ${filePath}")

		File csvFile = new File(filePath)
		if (!csvFile.exists())  throw new FileNotFoundException("[CsvHelper.removeTopRowsAndRenumber] File tidak ditemukan: ${filePath}")
		if (!csvFile.canRead()) throw new IOException("[CsvHelper.removeTopRowsAndRenumber] File tidak dapat dibaca: ${filePath}")

		List<String[]> allData  = readAll(filePath)
		String[] header         = allData.get(0)
		List<String[]> dataRows = allData.size() > 1
				? new ArrayList<>(allData.subList(1, allData.size()))
				: new ArrayList<>()

		KeywordUtil.logInfo("[CsvHelper.removeTopRowsAndRenumber] Data sebelum hapus: ${dataRows.size()} baris")

		if (removeCount >= dataRows.size()) {
			KeywordUtil.logInfo("[CsvHelper.removeTopRowsAndRenumber] removeCount >= jumlah data, menyisakan header saja")
			writeAll(filePath, [header])
			return
		}

		removeCount.times { dataRows.remove(0) }
		renumberFirstColumn(dataRows)

		List<String[]> updated = [[header], dataRows].flatten() as List<String[]>
		writeAll(filePath, updated)
		KeywordUtil.logInfo("[CsvHelper.removeTopRowsAndRenumber] Selesai. Sisa data: ${dataRows.size()} baris")
	}

	/**
	 * Menghapus beberapa baris data berdasarkan daftar indeks, lalu melakukan renumber.
	 * Indeks yang tidak valid (di luar jangkauan) diabaikan.
	 * @param filePath       Path absolut atau relatif ke file CSV.
	 * @param rowIndexes     List indeks baris data yang akan dihapus (dimulai dari 1).
	 * @throws FileNotFoundException jika file tidak ditemukan.
	 * @throws IOException           jika file tidak dapat dibaca.
	 */
	static void removeSpecificRowsAndRenumber(String filePath, List<Integer> rowIndexes) {
		KeywordUtil.logInfo("[CsvHelper.removeSpecificRowsAndRenumber] Hapus baris: ${rowIndexes} dari: ${filePath}")

		File csvFile = new File(filePath)
		if (!csvFile.exists())  throw new FileNotFoundException("[CsvHelper.removeSpecificRowsAndRenumber] File tidak ditemukan: ${filePath}")
		if (!csvFile.canRead()) throw new IOException("[CsvHelper.removeSpecificRowsAndRenumber] File tidak dapat dibaca: ${filePath}")

		List<String[]> allData  = readAll(filePath)
		String[] header         = allData.get(0)
		List<String[]> dataRows = allData.size() > 1
				? new ArrayList<>(allData.subList(1, allData.size()))
				: new ArrayList<>()

		KeywordUtil.logInfo("[CsvHelper.removeSpecificRowsAndRenumber] Data sebelum hapus: ${dataRows.size()} baris")

		List<Integer> validIndexes = rowIndexes
				.findAll { it > 0 && it <= dataRows.size() }
				.sort { -it } // descending agar index tidak bergeser saat remove

		if (validIndexes.isEmpty()) {
			KeywordUtil.logInfo("[CsvHelper.removeSpecificRowsAndRenumber] Tidak ada indeks valid, tidak ada yang dihapus")
			return
		}

		validIndexes.each { idx ->
			dataRows.remove(idx - 1)
			KeywordUtil.logInfo("[CsvHelper.removeSpecificRowsAndRenumber] Hapus baris ke-${idx}")
		}

		renumberFirstColumn(dataRows)

		List<String[]> updated = [[header], dataRows].flatten() as List<String[]>
		writeAll(filePath, updated)
		KeywordUtil.logInfo("[CsvHelper.removeSpecificRowsAndRenumber] Selesai. Sisa data: ${dataRows.size()} baris")
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	/**
	 * Melakukan renumber pada kolom pertama (index 0) dari list baris data.
	 * Nilai kolom pertama diisi dengan urutan 1, 2, 3, ...
	 * @param dataRows  List baris data (tanpa header).
	 */
	private static void renumberFirstColumn(List<String[]> dataRows) {
		dataRows.eachWithIndex { row, i ->
			if (row.length > 0) row[0] = String.valueOf(i + 1)
		}
	}
}
