package core

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.mobile.keyword.internal.MobileDriverFactory
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.util.CryptoUtil

import internal.GlobalVariable

import io.appium.java_client.AppiumBy
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement

import core.SafeActionsMobile

class Util {

	//============================================= DATA =============================================//
	/**
	 * decrypt string
	 */
	static String decryptKatalon(String enc) {
		if (enc == null) return null
		try {
			return CryptoUtil.decode(CryptoUtil.getDefault(enc))
		} catch (Exception e) {
			KeywordUtil.markWarning("Gagal dekripsi: " + e.getMessage())
			return enc
		}
	}

	//============================================= PERMISSION =============================================//
	/**
	 * Ketuk satu tombol izin jika ada.
	 * @return true kalau ada yang diketuk, false kalau tidak ada dialog izin.
	 */
	@Keyword
	static boolean tapOnePermissionIfPresent(int perElementTimeoutSec = 1) {
		KeywordUtil.logInfo("[Util.tapOnePermissionIfPresent] START timeout=${perElementTimeoutSec}")
		String[] possibleIds = [
			"com.android.permissioncontroller:id/permission_allow_foreground_only_button",
			"com.android.permissioncontroller:id/permission_allow_all_button",
			"com.android.permissioncontroller:id/permission_allow_button"
		]

		for (String id : possibleIds) {
			TestObject byId = new TestObject("perm_by_id")
			byId.addProperty("resource-id", ConditionType.EQUALS, id)

			if (SafeActionsMobile.safeWait(byId, perElementTimeoutSec)) {
				SafeActionsMobile.safeTap(byId, perElementTimeoutSec)
				KeywordUtil.logInfo("[Util.tapOnePermissionIfPresent] END → tapped: ${id}")
				return true
			}
		}
		KeywordUtil.logInfo("[Util.tapOnePermissionIfPresent] END → tidak ada dialog izin")
		return false
	}

	/**
	 * Loop untuk handle beberapa dialog izin.
	 */
	@Keyword
	static void handlePermissions(int maxDialogs = 6, int timeout = 1) {
		KeywordUtil.logInfo("[Util.handlePermissions] START maxDialogs=${maxDialogs} timeout=${timeout}")
		for (int i = 0; i < maxDialogs; i++) {
			boolean tapped = Util.tapOnePermissionIfPresent(timeout)
			if (!tapped) break
				Mobile.delay(0.5)
		}
		KeywordUtil.logInfo("[Util.handlePermissions] END")
	}

	//============================================= ANDROID DRIVER =============================================//
	/**
	 * Menunggu hingga package tertentu aktif di foreground.
	 * @param expectedPackageId  package yang diharapkan (mis. com.android.camera)
	 * @param retrySec           batas waktu dalam detik
	 * @param alternativePackageId package alternatif yang juga dianggap valid (opsional, default null)
	 */
	static void waitForPackageForeground(String expectedPackageId, int retrySec, String alternativePackageId = null) {
		int waited = 0

		def rawDriver = MobileDriverFactory.getDriver()
		if (!(rawDriver instanceof AndroidDriver)) {
			KeywordUtil.markFailedAndStop("Driver bukan AndroidDriver. Hanya didukung di Android.")
			return
		}

		AndroidDriver android = (AndroidDriver) rawDriver
		String currentPkg = android.getCurrentPackage()?.toString()

		String logTarget = alternativePackageId ? "${expectedPackageId} atau ${alternativePackageId}" : expectedPackageId
		KeywordUtil.logInfo("Menunggu package foreground: ${logTarget} (timeout: ${retrySec} detik)")

		while (!isExpectedPackage(currentPkg, expectedPackageId, alternativePackageId) && waited < retrySec) {
			rawDriver = MobileDriverFactory.getDriver()
			if (rawDriver instanceof AndroidDriver) {
				android = (AndroidDriver) rawDriver
				currentPkg = android.getCurrentPackage()?.toString()
			}
			Mobile.delay(1)
			waited++
		}

		if (!isExpectedPackage(currentPkg, expectedPackageId, alternativePackageId)) {
			KeywordUtil.markFailedAndStop(
					"Package '${logTarget}' tidak aktif dalam ${retrySec} detik. " +
					"Terakhir terdeteksi: '${currentPkg ?: 'null'}'"
					)
		} else {
			KeywordUtil.logInfo("Package aktif: ${currentPkg}")
		}
	}

	/**
	 * Helper untuk mengecek apakah package saat ini sesuai dengan expected atau alternatif.
	 */
	private static boolean isExpectedPackage(String currentPkg, String expectedPackageId, String alternativePackageId) {
		if (safeEqualsIgnoreCase(currentPkg, expectedPackageId)) return true
		if (alternativePackageId != null && safeEqualsIgnoreCase(currentPkg, alternativePackageId)) return true
		return false
	}

	//============================================= COMPARISON METHOD =============================================//
	/**
	 * Helper untuk mengecek apakah string null atau kosong (blank).
	 */
	static boolean isNullOrEmpty(String s) {
		return s == null || s.trim().isEmpty()
	}

	/**
	 * Helper untuk membandingkan string secara case-insensitive dan aman terhadap null.
	 */
	private static boolean safeEqualsIgnoreCase(String a, String b) {
		if (a == null || b == null) return false
		return a.equalsIgnoreCase(b)
	}

	//============================================= KOORDINAT =============================================//
	/**
	 * Mendapatkan koordinat [x, y] dari suatu object.
	 */
	static List<Integer> getKoordinatObject(TestObject object) {
		KeywordUtil.logInfo("[Util.getKoordinatObject] START object=${object?.getObjectId()}")
		int xKoordinat = SafeActionsMobile.safeGetElementLeftPosition(object)
		int yKoordinat = SafeActionsMobile.safeGetElementTopPosition(object)

		List<Integer> listKoordinat = [xKoordinat, yKoordinat]
		KeywordUtil.logInfo("[Util.getKoordinatObject] END → x=${xKoordinat} y=${yKoordinat}")
		return listKoordinat
	}

	//============================================= SCROLL =============================================//
	/**
	 * Scroll ke elemen berdasarkan resource-id (dan opsional teks).
	 */
	static void scrollToObjectByLocatorResourceId(String resId, String textContent = "", boolean isExactMatch = false) {
		KeywordUtil.logInfo("[Util.scrollToObjectByLocatorResourceId] START resId=${resId} textContent=${textContent} isExactMatch=${isExactMatch}")
		AppiumDriver driver = MobileDriverFactory.getDriver()

		// 1. Base selector (resource-id)
		String selector = "new UiSelector().resourceId(\"${resId}\")"

		// 2. Cek apakah ada teks yang ingin difilter
		if (textContent != null && !textContent.trim().isEmpty()) {
			if (isExactMatch) {
				// Jika isExactMatch diset true, cari yang sama persis
				selector += ".text(\"${textContent}\")"
			} else {
				// Jika isExactMatch false (bawaan), cari pakai contains
				selector += ".textContains(\"${textContent}\")"
			}
		}

		// 3. Rangkai kode UIAutomator
		String uiAutomatorCode = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(" + selector + ")"
		KeywordUtil.logInfo("[Util.scrollToObjectByLocatorResourceId] uiAutomator=${uiAutomatorCode}")

		// 4. Eksekusi
		driver.findElement(AppiumBy.androidUIAutomator(uiAutomatorCode))
		KeywordUtil.logInfo("[Util.scrollToObjectByLocatorResourceId] END")
	}

	/**
	 * Swipe layar ke arah tertentu menggunakan koordinat Y relatif terhadap tinggi layar.
	 * Arah scroll ditentukan oleh nilai startYRatio vs endYRatio:
	 *   startYRatio > endYRatio → scroll ke atas (konten naik, elemen di bawah tampil)
	 *   startYRatio < endYRatio → scroll ke bawah (konten turun, elemen di atas tampil)
	 * Dimensi layar diambil secara otomatis — pemanggil tidak perlu meneruskan deviceWidth/deviceHeight.
	 * @param startYRatio Rasio Y mulai swipe dari tinggi layar (default: 0.8)
	 * @param endYRatio   Rasio Y akhir swipe dari tinggi layar (default: 0.7)
	 */
	static void swipeScroll(double startYRatio = 0.8, double endYRatio = 0.7) {
		KeywordUtil.logInfo("[Util.swipeScroll] START startYRatio=${startYRatio} endYRatio=${endYRatio}")
		int deviceHeight = SafeActionsMobile.safeGetDeviceHeight()
		int deviceWidth  = (int) Math.round(SafeActionsMobile.safeGetDeviceWidth() * 0.08)
		SafeActionsMobile.safeSwipe(deviceWidth, (int) Math.round(deviceHeight * startYRatio), deviceWidth, (int) Math.round(deviceHeight * endYRatio))
		KeywordUtil.logInfo("[Util.swipeScroll] END")
	}

	//============================================= DEVICE INFO =============================================//
	/**
	 * Mendapatkan package ID aplikasi yang sedang aktif.
	 */
	static String getApkPackageId() {
		KeywordUtil.logInfo("[Util.getApkPackageId] START")
		String packageId
		def rawDriver = MobileDriverFactory.getDriver()
		if (rawDriver instanceof AndroidDriver) {
			def android = (AndroidDriver) rawDriver
			packageId = android.getCurrentPackage()
			KeywordUtil.logInfo("[Util.getApkPackageId] packageId=${packageId}")
		}
		KeywordUtil.logInfo("[Util.getApkPackageId] END → ${packageId}")
		return packageId
	}

	/**
	 * Mendapatkan waktu perangkat saat ini.
	 */
	static String getDeviceTime() {
		KeywordUtil.logInfo("[Util.getDeviceTime] START")
		AndroidDriver driver = MobileDriverFactory.getDriver()
		String deviceDate = driver.getDeviceTime()
		KeywordUtil.logInfo("[Util.getDeviceTime] END → ${deviceDate}")
		return deviceDate
	}

	/**
	 * Mendapatkan bahasa perangkat.
	 */
	static String getDeviceLanguage() {
		KeywordUtil.logInfo("[Util.getDeviceLanguage] START")
		AndroidDriver driver = MobileDriverFactory.getDriver()
		String udid = driver.getCapabilities().getCapability("udid")

		def p = new ProcessBuilder(
				"adb", "-s", udid, "shell", "getprop", "persist.sys.locale"
				).start()

		p.waitFor()
		String language = p.inputStream.getText("UTF-8").trim().split("-")[0]
		KeywordUtil.logInfo("[Util.getDeviceLanguage] END → ${language}")
		return language
	}

	/**
	 * Mencari semua elemen berdasarkan xpath.
	 */
	static List<WebElement> findElementsByXpath(String xpath) {
		KeywordUtil.logInfo("[Util.findElementsByXpath] START xpath=${xpath}")
		AppiumDriver driver = MobileDriverFactory.getDriver()
		List<WebElement> elements = driver.findElements(By.xpath(xpath))
		KeywordUtil.logInfo("[Util.findElementsByXpath] END → ditemukan ${elements.size()} elemen")
		return elements
	}

	/**
	 * Menunggu sampai aplikasi benar-benar ada di foreground (aktif).
	 * Menunggu tanpa batas waktu.
	 */
	static void waitAppsActive(String apkId) {
		KeywordUtil.logInfo("[Util.waitAppsActive] START apkId=${apkId}")
		def rawDriver = MobileDriverFactory.getDriver()
		if (rawDriver instanceof AndroidDriver) {
			def android = (AndroidDriver) rawDriver
			String packageId = android.getCurrentPackage().toString()
			while (!packageId.equalsIgnoreCase(apkId)) {
				rawDriver = MobileDriverFactory.getDriver()
				if (rawDriver instanceof AndroidDriver) {
					android = (AndroidDriver) rawDriver
					packageId = android.getCurrentPackage().toString()
				}
				Mobile.delay(1)
			}
		}
		KeywordUtil.logInfo("[Util.waitAppsActive] END")
	}

	/**
	 * Mencetak semua elemen yang tampil di layar ke console.
	 */
	static void printAllScreen() {
		KeywordUtil.logInfo("[Util.printAllScreen] START")
		String pageSource = MobileDriverFactory.getDriver().getPageSource()
		def xml = new XmlSlurper().parseText(pageSource)

		xml."**".each { node ->
			println "CLASS       : ${node.name()}"
			println "TEXT        : ${node.@text}"
			println "RESOURCE-ID : ${node.@'resource-id'}"
			println "CONTENT-DESC: ${node.@'content-desc'}"
			println "-------------------------------------"
		}
	}

	/**
	 * Mencetak semua elemen yang tampil di layar ke file txt di folder screenshotPath.
	 * File disimpan di: GlobalVariable.pathCapture + screenshotPath + /screen_dump_<timestamp>.txt
	 */
	static void printAllScreenToFile(String screenshotPath) {
		KeywordUtil.logInfo("[Util.printAllScreenToFile] START screenshotPath=${screenshotPath}")
		String pageSource = MobileDriverFactory.getDriver().getPageSource()
		def xml = new XmlSlurper().parseText(pageSource)

		StringBuilder sb = new StringBuilder()
		xml."**".each { node ->
			sb.append("CLASS       : ${node.name()}\n")
			sb.append("TEXT        : ${node.@text}\n")
			sb.append("RESOURCE-ID : ${node.@'resource-id'}\n")
			sb.append("CONTENT-DESC: ${node.@'content-desc'}\n")
			sb.append("-------------------------------------\n")
		}

		String dir = GlobalVariable.SCREENSHOT_PATH + screenshotPath
		new File(dir).mkdirs()
		String timestamp = new Date().format("yyyyMMdd_HHmmss")
		String filePath = "${dir}/screen_dump_${timestamp}.txt"
		new File(filePath).text = sb.toString()

		KeywordUtil.logInfo("[Util.printAllScreenToFile] END → disimpan ke: ${filePath}")
	}

	//============================================= APK MANAGEMENT =============================================//
	/**
	 * Uninstall APK dari device berdasarkan package ID-nya via ADB.
	 * Tidak melempar exception jika package memang sudah tidak terpasang — hanya log info.
	 * @param apkId  Package ID aplikasi (misal: com.example.Apps).
	 */
	static void uninstallApk(String apkId) {
		if (!apkId?.trim()) {
			KeywordUtil.markFailedAndStop("[Util.uninstallApk] apkId tidak boleh kosong.")
			return
		}

		def rawDriver = MobileDriverFactory.getDriver()
		if (!(rawDriver instanceof AndroidDriver)) {
			KeywordUtil.markFailedAndStop("[Util.uninstallApk] Driver bukan AndroidDriver.")
			return
		}

		AndroidDriver android = (AndroidDriver) rawDriver
		String udid = android.getCapabilities().getCapability("udid")
		KeywordUtil.logInfo("[Util.uninstallApk] Mulai uninstall '${apkId}' dari device: ${udid}")

		def process = new ProcessBuilder("adb", "-s", udid, "uninstall", apkId)
				.redirectErrorStream(true)
				.start()
		process.waitFor()
		String output = process.inputStream.getText("UTF-8").trim()
		KeywordUtil.logInfo("[Util.uninstallApk] Output ADB: ${output}")

		if (output.toLowerCase().contains("success")) {
			KeywordUtil.logInfo("[Util.uninstallApk] Berhasil uninstall '${apkId}'")
		} else if (output.toLowerCase().contains("not installed")) {
			KeywordUtil.logInfo("[Util.uninstallApk] Package '${apkId}' tidak terpasang di device, dilewati.")
		} else {
			KeywordUtil.markFailed("[Util.uninstallApk] Gagal uninstall '${apkId}': ${output}")
		}
	}

	/**
	 * Install APK ke device dari path yang ditentukan.
	 * Jika file tidak ditemukan, method akan gagal dengan pesan path yang dicoba.
	 * @param apkPath      Folder tempat APK berada (misal: "External Data/SQUAD/APK/").
	 * @param apkFileName  Nama file APK (misal: "apps-1.16.0.apk").
	 */
	static void installApk(String apkPath, String apkFileName) {
		if (!apkPath?.trim() || !apkFileName?.trim()) {
			KeywordUtil.markFailedAndStop("[Util.installApk] apkPath dan apkFileName tidak boleh kosong.")
			return
		}

		File apkFile = new File(apkPath, apkFileName)
		KeywordUtil.logInfo("[Util.installApk] Path APK: ${apkFile.absolutePath}")

		if (!apkFile.exists()) {
			KeywordUtil.markFailedAndStop("[Util.installApk] File tidak ditemukan: ${apkFile.absolutePath}")
			return
		}

		def rawDriver = MobileDriverFactory.getDriver()
		if (!(rawDriver instanceof AndroidDriver)) {
			KeywordUtil.markFailedAndStop("[Util.installApk] Driver bukan AndroidDriver.")
			return
		}

		AndroidDriver android = (AndroidDriver) rawDriver
		KeywordUtil.logInfo("[Util.installApk] Mulai install '${apkFileName}' ke device...")
		android.installApp(apkFile.absolutePath)
		KeywordUtil.logInfo("[Util.installApk] Berhasil install '${apkFileName}'")
	}

	//============================================= TABLE =============================================//
	/**
	 * Menghapus semua baris pada tabel dengan mengetuk tombol hapus satu per satu.
	 *
	 * Jumlah iterasi ditentukan dari jumlah elemen yang ditemukan via {@code xpathRows}.
	 * Setiap iterasi selalu mengetuk posisi {@code "1"} karena setelah
	 * sebuah baris dihapus, baris berikutnya bergeser naik ke posisi tersebut.
	 *
	 * Contoh penggunaan:
	 * <pre>
	 *   // Tanpa afterHapus
	 *   Util.hapusSemuaIsiTabel(
	 *       HalamanDataPendapatanSurveiWawancara.tableDaftarPendapatanLainnya().value,
	 *       { String idx -> HalamanDataPendapatanSurveiWawancara.textViewHapusTabel(idx).testObject }
	 *   )
	 *
	 *   // Dengan afterHapus — misal delay setelah tiap hapus
	 *   Util.hapusSemuaIsiTabel(
	 *       HalamanDataPendapatanSurveiWawancara.tableDaftarPendapatanLainnya().value,
	 *       { String idx -> HalamanDataPendapatanSurveiWawancara.textViewHapusTabel(idx).testObject },
	 *       { Mobile.delay(0.5) }
	 *   )
	 *
	 *   // Dengan afterHapus — misal konfirmasi dialog setelah tiap hapus
	 *   Util.hapusSemuaIsiTabel(
	 *       HalamanDataPendapatanSurveiWawancara.tableDaftarPendapatanLainnya().value,
	 *       { String idx -> HalamanDataPendapatanSurveiWawancara.textViewHapusTabel(idx).testObject },
	 *       { SafeActionsMobile.safeTapOrFail(HalamanKonfirmasi.btnYa().testObject) }
	 *   )
	 * </pre>
	 *
	 * @param xpathRows      XPath untuk menghitung jumlah baris yang ada di tabel.
	 * @param getHapusObject Closure yang menerima index (String) dan mengembalikan TestObject tombol hapus.
	 * @param afterHapus     Closure opsional yang dipanggil setelah setiap penghapusan (boleh {@code null}).
	 *                       Contoh: {@code { Mobile.delay(0.5) }} atau {@code { Util.handlePermissions() }}
	 */
	@Keyword
	static void hapusSemuaIsiTabel(String xpathRows, Closure<TestObject> getHapusObject, Closure afterHapus = null) {
		KeywordUtil.logInfo("[Util.hapusSemuaIsiTabel] START xpathRows=${xpathRows}")
		String posisiHapus = "1"

		AppiumDriver driver = MobileDriverFactory.getDriver()
		List<WebElement> elements = driver.findElements(By.xpath(xpathRows))
		int jumlah = elements.size()
		KeywordUtil.logInfo("[Util.hapusSemuaIsiTabel] Jumlah baris yang akan dihapus: ${jumlah}")

		for (int i = 1; i <= jumlah; i++) {
			Mobile.delay(0.5)
			TestObject hapusObject = getHapusObject(posisiHapus)
			KeywordUtil.logInfo("[Util.hapusSemuaIsiTabel] Hapus iterasi ke-${i} pada posisi ${posisiHapus}")
			SafeActionsMobile.safeTapOrFail(hapusObject)
			afterHapus?.call()
		}

		KeywordUtil.logInfo("[Util.hapusSemuaIsiTabel] END → ${jumlah} baris berhasil dihapus")
	}

	//============================================= HIGHLIGHT TEST OBJECT (WEB) =============================================//
	static void highlightElement(WebElement element) {
		String borderColor = GlobalVariable.BORDER_COLOR
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver()

		js.executeScript("""
			arguments[0].style.border='4px solid ${borderColor}';
	    """, element)
	}

	static void removeHighlight(WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver()

		js.executeScript("""
	        arguments[0].style.border='';
	    """, element)
	}
}
