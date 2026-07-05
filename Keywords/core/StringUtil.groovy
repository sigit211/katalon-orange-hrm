package core

final class StringUtil {

	private StringUtil() {}

	//============================================= LIST =============================================//
	/**
	 * Mengubah string menjadi list (hapus spasi dan bracket).
	 */
	static List<String> transformStringToList(String data) {
		data = data.replaceAll(/[\[\]\s]/, '')
		List<String> listData = data ? data.split(',') as List<String> : []
		return listData
	}

	//============================================= GENERATE =============================================//
	/**
	 * Menghasilkan string dengan setiap karakter dipisahkan spasi, diulang sebanyak spaceCount.
	 * Contoh: StringUtil.repeatCharSeparatedBySpace("halo", 4) → h a l o h a l o h a l o h a l o
	 */
	static String repeatCharSeparatedBySpace(String input, int spaceCount) {
		def result = (1..spaceCount).collect { input }.join('')
		result = result.collect { it }.join(' ')
		return result
	}

	/**
	 * Menghasilkan string alfabet dengan panjang tertentu (a-z berulang).
	 */
	static String generateStringAlphabet(int length) {
		if (length <= 0) return ""
		String alphabet = "abcdefghijklmnopqrstuvwxyz"
		int n = alphabet.length()
		StringBuilder sb = new StringBuilder(length)
		for (int i = 0; i < length; i++) {
			sb.append(alphabet.charAt(i % n))
		}
		return sb.toString()
	}

	//============================================= CURRENCY =============================================//
	/**
	 * Membersihkan string nominal menjadi digit-only (opsional minus),
	 * dengan membuang bagian desimal.
	 *
	 * Aturan desimal:
	 *  - Indonesia: koma (,) sebagai desimal. Jika ada koma → buang koma dan semua setelahnya.
	 *  - Jika tidak ada koma, tetapi ada titik terakhir yang terlihat sebagai desimal
	 *    (contoh: di belakang titik ada 1-2 digit), maka buang titik dan semua setelahnya.
	 *  - Titik yang lain dianggap pemisah ribuan dan akan dibuang.
	 */
	static def cleanCurrencyToDigits(String raw) {
		if (raw == null) return ""

		String s = raw.trim()
		boolean isNegative = s.contains("-")

		// 1) Hilangkan semua yang bukan digit/koma/titik/minus sementara
		s = s.replaceAll("[^0-9,.-]", "")

		// 2) BUANG DESIMAL
		// 2a) Jika ada koma (format Indonesia), anggap koma sebagai desimal → buang koma dan sesudahnya
		if (s.contains(",")) {
			int idx = s.indexOf(",")
			s = s.substring(0, idx)
		} else {
			// 2b) Tidak ada koma, cek apakah ada titik desimal
			int lastDot = s.lastIndexOf(".")
			if (lastDot != -1) {
				String after = s.substring(lastDot + 1)
				// Jika setelah titik ada 1-2 digit, anggap ini desimal
				if (after.matches("\\d{1,2}")) {
					s = s.substring(0, lastDot)
				}
			}
		}

		// 3) Buang semua pemisah ribuan (titik/koma sisa) & karakter non-digit
		s = s.replaceAll("[^0-9]", "")

		if (s.isEmpty()) return ""

		// 4) Normalisasi minus: tampilkan minus hanya jika ada digit non-nol
		boolean allZero = s.chars().allMatch { it == (int) '0' }
		if (isNegative && !allZero) {
			return "-" + s
		}
		return s
	}

	/**
	 * Membersihkan string nominal dan mengembalikan Long (null jika tidak bisa parse).
	 */
	static def parseCurrencyToLong(String raw) {
		String cleaned = cleanCurrencyToDigits(raw)
		if (cleaned == null || cleaned.isEmpty() || cleaned == "-") return null
		try {
			return Long.valueOf(cleaned)
		} catch (NumberFormatException ex) {
			return null
		}
	}
}
