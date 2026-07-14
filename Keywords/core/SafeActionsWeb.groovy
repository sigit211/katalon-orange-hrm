package core

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.apache.commons.io.FileUtils
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebElement

import internal.GlobalVariable
import core.Util

/**
 * SafeActions — wrapper retry-safe untuk semua interaksi WebUI di Katalon.
 *
 * Setiap method tersedia dalam dua varian:
 *   - Versi biasa (safe*)  : mengembalikan boolean true/false jika berhasil/gagal.
 *                            Test case TIDAK berhenti walaupun gagal.
 *   - Versi OrFail (*OrFail): melempar StepFailedException jika semua retry habis.
 *                            Test case BERHENTI dan ditandai FAILED.
 *
 * Pola retry pada setiap method:
 *   1. Coba jalankan aksi.
 *   2. Jika berhasil → return true.
 *   3. Jika gagal  → tunggu DEFAULT_DELAY detik, lalu ulangi sampai retryCount habis.
 *   4. Jika semua retry habis → markFailed / markWarning (tergantung method).
 */
public class SafeActionsWeb {
	/** Jumlah percobaan default jika sebuah aksi gagal. */
	static final int DEFAULT_RETRY = 2

	/** Jeda (detik) antar percobaan supaya browser punya waktu merespons. */
	static final int DEFAULT_DELAY = 1

	/** Timeout default (detik) yang dipakai saat menunggu elemen. Nilainya dari GlobalVariable.TIMEOUT. */
	static final int DEFAULT_TIMEOUT = GlobalVariable.TIMEOUT

	/** Waktu tunggu maksimal (detik) khusus untuk waitForElementPresent versi OrFail. Nilainya dari GlobalVariable.MAX_WAIT_SECONDS. */
	static final int MAX_WAIT_SECONDS = GlobalVariable.MAX_WAIT_SECONDS

	/**
	 * Internal helper untuk retry.
	 * @param tag        Nama method untuk logging.
	 * @param failValue  Nilai yang dikembalikan jika semua retry gagal.
	 * @param retryCount Jumlah maksimum percobaan.
	 * @param action     Closure yang dieksekusi setiap percobaan.
	 */
	private static def withRetry(String tag, def failValue, int retryCount, Closure action) {
		for (int attempt = 1; attempt <= retryCount; attempt++) {
			try {
				return action.call(attempt)
			} catch (Exception e) {
				KeywordUtil.logInfo("[${tag}] Gagal attempt ${attempt}/${retryCount} | error='${e.message}'")
				if (attempt < retryCount) {
					WebUI.delay(DEFAULT_DELAY)
				}
			}
		}
		return failValue
	}

	// ======================== SAFE OPEN BROWSER ==============================
	/**
	 * Buka browser ke URL yang ditentukan lalu maximize window, dengan retry otomatis.
	 * @param url        URL yang akan dibuka
	 * @param retryCount jumlah maksimal percobaan
	 * @return true jika berhasil, false jika gagal setelah semua retry
	 */
	static boolean safeOpenBrowser(String url, int retryCount = DEFAULT_RETRY) {
		boolean opened = withRetry('SafeOpenBrowser', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeOpenBrowser] Membuka browser ke: '${url}' | attempt=${attempt}/${retryCount}")
			WebUI.openBrowser(url)
			WebUI.maximizeWindow()
			KeywordUtil.logInfo("[SafeOpenBrowser] Berhasil membuka browser dan maximize window pada attempt ${attempt}")
			true
		}
		if (!opened) {
			KeywordUtil.markFailed("[SafeOpenBrowser] Gagal membuka browser setelah ${retryCount}x retry: ${url}")
			KeywordUtil.logInfo("[SafeOpenBrowser] STATUS: FAILED setelah ${retryCount}x retry untuk URL: ${url}")
		}
		return opened
	}

	/**
	 * Buka browser ke URL — lempar StepFailedException jika gagal.
	 * Gunakan saat browser wajib berhasil terbuka untuk melanjutkan test.
	 */
	static void safeOpenBrowserOrFail(String url, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeOpenBrowserOrFail] Eksekusi: url='${url}' | retry=${retryCount}")
		boolean opened = safeOpenBrowser(url, retryCount)
		if (!opened) {
			KeywordUtil.logInfo("[SafeOpenBrowserOrFail] Gagal membuka browser, melempar exception: ${url}")
			throw new StepFailedException("[SafeOpenBrowserOrFail] Gagal membuka browser: ${url}")
		}
		KeywordUtil.logInfo("[SafeOpenBrowserOrFail] Browser berhasil dibuka: ${url}")
	}

	// ======================== SAFE CLICK ==============================
	/**
	 * Klik elemen dengan retry otomatis.
	 * Menunggu elemen hadir (waitForElementPresent) sebelum klik.
	 * @param to        TestObject target
	 * @param timeout   waktu tunggu per percobaan (detik)
	 * @param retryCount jumlah maksimal percobaan
	 * @return true jika klik berhasil, false jika gagal setelah semua retry
	 */
	static boolean safeClick(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean clicked = withRetry('SafeClick', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeClick] Mulai click: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			WebUI.waitForElementClickable(to, timeout, FailureHandling.OPTIONAL)
			WebUI.click(to)
			KeywordUtil.logInfo("[SafeClick] Berhasil click: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!clicked) {
			KeywordUtil.markFailed("[SafeClick] Gagal click setelah ${retryCount}x retry : ${to?.getObjectId()}")
			KeywordUtil.logInfo("[SafeClick] STATUS: FAILED setelah ${retryCount}x retry untuk: ${to?.getObjectId()}")
		}
		return clicked
	}

	/**
	 * Klik elemen — lempar StepFailedException jika gagal.
	 * Gunakan saat klik wajib berhasil untuk melanjutkan test.
	 */
	static void safeClickOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeClickOrFail] Eksekusi: ${to?.getObjectId()} | timeout=${timeout} | retry=${retryCount}")
		boolean clicked = safeClick(to, timeout, retryCount)
		if (!clicked) {
			KeywordUtil.logInfo("[SafeClickOrFail] Click gagal, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[SafeClickOrFail] Click gagal: ${to.getObjectId()}")
			// atau: KeywordUtil.markFailedAndStop("...") kalau mau hard-stop
		}
		KeywordUtil.logInfo("[SafeClickOrFail] Click sukses: ${to?.getObjectId()}")
	}

	// ======================== SAFE SET TEXT ==========================
	/**
	 * Ketik teks ke dalam field input dengan retry otomatis.
	 * Menunggu elemen hadir sebelum setText.
	 * @param to    TestObject field input
	 * @param text  teks yang akan diketik
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeSetText(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean textSet = withRetry('SafeSetText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSetText] Mulai setText: ${to?.getObjectId()} | text='${text}' | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			WebUI.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)
			WebUI.setText(to, text)
			KeywordUtil.logInfo("[SafeSetText] Berhasil setText: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!textSet) {
			KeywordUtil.markFailed("[SafeSetText] Gagal setText setelah ${retryCount}x retry : ${to?.getObjectId()}")
		}
		return textSet
	}

	/**
	 * Ketik teks ke field input — lempar StepFailedException jika gagal.
	 */
	static void safeSetTextOrFail(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeSetTextOrFail] Eksekusi: to=${to?.getObjectId()} | text='${text}' | timeout=${timeout} | retry=${retryCount}")
		if (!safeSetText(to, text, timeout, retryCount)) {
			KeywordUtil.logInfo("[SafeSetTextOrFail] SetText gagal, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[SafeSetTextOrFail] SetText gagal: ${to.getObjectId()}")
		}
		KeywordUtil.logInfo("[SafeSetTextOrFail] SetText sukses: ${to?.getObjectId()}")
	}

	// ======================== SAFE SEND KEYS =========================
	/**
	 * Kirim keystroke ke elemen (simulasi keyboard) dengan retry.
	 * Berbeda dengan setText: sendKeys mengirim event keyboard satu per satu,
	 * cocok untuk field yang memerlukan trigger event JS (misal: autocomplete).
	 * @param to    TestObject target
	 * @param text  string keystroke yang dikirim
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeSendKeys(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean keysSent = withRetry('SafeSendKeys', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSendKeys] Mulai sendKeys: ${to?.getObjectId()} | text='${text}' | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			WebUI.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)
			WebUI.sendKeys(to, text)
			KeywordUtil.logInfo("[SafeSendKeys] Berhasil sendKeys: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!keysSent) {
			KeywordUtil.markFailed("[SafeSendKeys] Gagal sendKeys setelah ${retryCount}x retry : ${to?.getObjectId()}")
		}
		return keysSent
	}

	/**
	 * Kirim keystroke — lempar StepFailedException jika gagal.
	 */
	static void safeSendKeysOrFail(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeSendKeysOrFail] Eksekusi: to=${to?.getObjectId()} | text='${text}' | timeout=${timeout} | retry=${retryCount}")
		if (!safeSendKeys(to, text, timeout, retryCount)) {
			KeywordUtil.logInfo("[SafeSendKeysOrFail] SendKeys gagal, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[SafeSendKeysOrFail] SendKeys gagal: ${to.getObjectId()}")
		}
		KeywordUtil.logInfo("[SafeSendKeysOrFail] SendKeys sukses: ${to?.getObjectId()}")
	}

	// ======================== SAFE CLEAR TEXT =========================
	/**
	 * Hapus isi field input dengan retry.
	 * Menunggu elemen hadir sebelum clearText.
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeClearText(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean cleared = withRetry('SafeClearText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeClearText] Mulai clearText: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			WebUI.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)
			WebUI.clearText(to)
			KeywordUtil.logInfo("[SafeClearText] Berhasil clearText: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!cleared) {
			KeywordUtil.markFailed("[SafeClearText] Gagal clearText setelah ${retryCount}x retry : ${to?.getObjectId()}")
		}
		return cleared
	}

	// ======================== SAFE WAIT FOR ELEMENT PRESENT ==============================
	/**
	 * Tunggu sampai elemen hadir di DOM/layar, dengan retry.
	 * Setiap retry menunggu timeout detik sebelum mencoba lagi.
	 * @return true jika elemen ditemukan, false jika tidak ditemukan setelah semua retry
	 */
	static boolean safeWaitForElementPresent(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean present = withRetry('safeWaitForElementPresent', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeWaitForElementPresent] Menunggu element present: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!WebUI.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)) {
				throw new Exception('Element belum present')
			}
			KeywordUtil.logInfo("[safeWaitForElementPresent] Element present: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!present) {
			KeywordUtil.logInfo("[safeWaitForElementPresent] STATUS: NOT FOUND setelah ${retryCount}x attempt untuk: ${to?.getObjectId()}")
		}
		return present
	}

	/**
	 * Tunggu sampai elemen hadir — lempar StepFailedException jika tidak ditemukan.
	 * @param retryCount    jumlah percobaan
	 * @param maxWaitSeconds waktu tunggu per percobaan (detik)
	 */
	static void safeWaitForElementPresentOrFail(TestObject to, int retryCount, int maxWaitSeconds = MAX_WAIT_SECONDS) {
		KeywordUtil.logInfo("[safeWaitForElementPresentOrFail] Cek element present: ${to?.getObjectId()} | retry=${retryCount} | maxWaitSeconds=${maxWaitSeconds}")
		if (!safeVerifyElementPresent(to, maxWaitSeconds, retryCount)) {
			KeywordUtil.logInfo("[safeWaitForElementPresentOrFail] Tidak ditemukan, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[safeWaitForElementPresentOrFail] Object '${to.getObjectId()}' tidak ditemukan dalam ${maxWaitSeconds*retryCount} detik!")
		}
		KeywordUtil.logInfo("[safeWaitForElementPresentOrFail] Element ditemukan: ${to?.getObjectId()}")
	}

	// ======================== SAFE TAKE SCREENSHOT ========================
	/**
	 * Ambil screenshot dan simpan ke path yang ditentukan, dengan retry.
	 * Jika gagal setelah semua retry, hanya markWarning (test TIDAK berhenti).
	 * @param path path lengkap file output screenshot (mis: "Screenshot/TC_01.png")
	 * @return true jika screenshot berhasil, false jika gagal
	 */
	static boolean safeTakeScreenshot(String path, TestObject to = null) {
		int retryCount = DEFAULT_RETRY
		// Normalkan ke absolute path
		File destFile = new File(path)
		if (!destFile.isAbsolute()) {
			destFile = new File(RunConfiguration.getProjectDir(), path)
		}
		// Pastikan folder tujuan ada
		destFile.parentFile?.mkdirs()
		String absolutePath = destFile.absolutePath

		boolean screenshotTaken = withRetry('SafeTakeScreenshot', false, retryCount) { int attempt ->
			WebElement element = null
			try {
				if (to != null) {
					KeywordUtil.logInfo("[SafeTakeScreenshot] HighlightElement sebelum screenshot | attempt=${attempt}/${retryCount}")
					element = WebUiCommonHelper.findWebElement(to, 10)
					Util.highlightElement(element)
					WebUI.delay(1)
				}
				KeywordUtil.logInfo("[SafeTakeScreenshot] Ambil screenshot ke: '${absolutePath}' | attempt=${attempt}/${retryCount}")
				File srcFile = ((TakesScreenshot) DriverFactory.getWebDriver()).getScreenshotAs(OutputType.FILE)
				FileUtils.copyFile(srcFile, destFile)
				KeywordUtil.logInfo("[SafeTakeScreenshot] Berhasil screenshot ke '${absolutePath}' pada attempt ${attempt}")
				true
			} finally {
				if (element != null) {
					Util.removeHighlight(element)
				}
			}
		}

		if (!screenshotTaken) {
			KeywordUtil.markWarning("[SafeTakeScreenshot] Screenshot gagal setelah ${retryCount}x retry")
			KeywordUtil.logInfo("[SafeTakeScreenshot] STATUS: FAILED setelah ${retryCount}x retry ke '${absolutePath}'")
		}
		return screenshotTaken
	}

	static boolean safeScrollBy(int startX, int startY, int endX, int endY, int retryCount = DEFAULT_RETRY) {
		int deltaX = endX - startX
		int deltaY = endY - startY
		boolean scrollSuccess = withRetry('SafeScrollBy', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeScrollBy] Mulai scroll (${startX},${startY}) -> (${endX},${endY}) | attempt=${attempt}/${retryCount}")
			WebUI.executeJavaScript("window.scrollBy(arguments[0], arguments[1])", [deltaX, deltaY])
			KeywordUtil.logInfo("[SafeScrollBy] Berhasil scroll pada attempt ${attempt}: delta=(${deltaX},${deltaY})")
			true
		}
		if (!scrollSuccess) {
			KeywordUtil.markFailed("[SafeScrollBy] Gagal scroll setelah ${retryCount}x retry")
			KeywordUtil.logInfo("[SafeScrollBy] STATUS: FAILED setelah ${retryCount}x retry")
		}
		return scrollSuccess
	}

	/**
	 * Scroll halaman — lempar StepFailedException jika gagal.
	 */
	static void safeScrollByOrFail(int startX, int startY, int endX, int endY, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeScrollByOrFail] Eksekusi scroll: (${startX},${startY}) -> (${endX},${endY}) | retry=${retryCount}")
		if (!safeScrollBy(startX, startY, endX, endY, retryCount)) {
			KeywordUtil.logInfo("[SafeScrollByOrFail] Scroll gagal, melempar exception")
			throw new StepFailedException("[SafeScrollByOrFail] Scroll gagal (${startX},${startY}) -> (${endX},${endY})")
		}
		KeywordUtil.logInfo("[SafeScrollByOrFail] Scroll sukses: (${startX},${startY}) -> (${endX},${endY})")
	}

	// ======================== SAFE SCROLL UNTIL VISIBLE ==============================
	/**
	 * Scroll berulang ke arah tertentu sampai elemen target terlihat di layar.
	 * X otomatis ditentukan dari tengah lebar window.
	 * Arah scroll ditentukan dari nilai startY vs endY:
	 *   - startY < endY → scroll ke atas (geser konten ke bawah)
	 *   - startY > endY → scroll ke bawah (geser konten ke atas)
	 * @param to        TestObject yang dicari
	 * @param startY    koordinat Y awal jari/pointer
	 * @param endY      koordinat Y akhir jari/pointer
	 * @param maxSwipes batas maksimal scroll sebelum menyerah
	 * @return true jika elemen ditemukan, false jika tidak ditemukan
	 */
	static boolean safeScrollUntilVisible(TestObject to, int startY, int endY, int maxSwipes = 20) {
		KeywordUtil.logInfo("[safeScrollUntilVisible] Mulai: target=${to?.getObjectId()} | startY=${startY} | endY=${endY} | maxSwipes=${maxSwipes}")
		if (to == null) {
			KeywordUtil.markFailed("[safeScrollUntilVisible] TestObject tidak boleh null")
			KeywordUtil.logInfo("[safeScrollUntilVisible] STATUS: FAILED karena TestObject null")
			return false
		}

		String arahScroll = (startY <= endY) ? "ke atas" : "ke bawah"
		KeywordUtil.logInfo("[safeScrollUntilVisible] Arah scroll ditentukan: ${arahScroll}")

		int windowWidth = safeGetWindowWidth()
		if (windowWidth <= 0) {
			try {
				KeywordUtil.logInfo("[safeScrollUntilVisible] Fallback getWindowWidth() dari JavaScript")
				windowWidth = ((Number) WebUI.executeJavaScript("return window.innerWidth", null)).intValue()
				KeywordUtil.logInfo("[safeScrollUntilVisible] windowWidth (fallback)=${windowWidth}")
			} catch (Exception ex) {
				KeywordUtil.markFailed("[safeScrollUntilVisible] Gagal mendapatkan lebar window: ${ex.message}")
				KeywordUtil.logInfo("[safeScrollUntilVisible] STATUS: FAILED saat ambil windowWidth | error='${ex.message}'")
				return false
			}
		}
		int centerX = Math.round(windowWidth / 2)
		KeywordUtil.logInfo("[safeScrollUntilVisible] centerX=${centerX}")

		if (safeVerifyElementPresent(to, 1, 1)) {
			KeywordUtil.logInfo("[safeScrollUntilVisible] Objek sudah terlihat tanpa perlu scroll")
			return true
		}

		for (int attempt = 1; attempt <= maxSwipes; attempt++) {
			KeywordUtil.logInfo("[safeScrollUntilVisible] Scroll ke ${arahScroll} attempt ${attempt}: (${centerX}, ${startY}) -> (${centerX}, ${endY})")
			boolean scrolled = safeScrollBy(centerX, startY, centerX, endY)
			if (!scrolled) {
				KeywordUtil.logInfo("[safeScrollUntilVisible] Scroll gagal pada attempt ${attempt}, lanjut mencoba lagi")
			}

			boolean visible = safeVerifyElementPresent(to, 1, 1)
			KeywordUtil.logInfo("[safeScrollUntilVisible] Cek visibilitas setelah scroll attempt ${attempt}: visible=${visible}")
			if (visible) {
				KeywordUtil.logInfo("[safeScrollUntilVisible] Objek ditemukan pada attempt ${attempt}")
				return true
			}
		}

		KeywordUtil.logInfo("[safeScrollUntilVisible] Objek belum ditemukan setelah ${maxSwipes}x scroll")
		return false
	}

	// ======================== SAFE SCROLL UNTIL VISIBLE BY COORDINATE ==============================
	/**
	 * Scroll berulang ke koordinat yang ditentukan secara eksplisit sampai elemen terlihat.
	 * Berbeda dengan safeScrollUntilVisible: startX/endX bisa ditentukan manual
	 * (berguna untuk scroll di dalam panel/container tertentu, bukan seluruh halaman).
	 * Jika startX/endX tidak diberikan (nilai < 0), otomatis pakai tengah window.
	 * @param to        TestObject yang dicari
	 * @param startX    koordinat X awal (-1 = otomatis)
	 * @param endX      koordinat X akhir (-1 = otomatis)
	 */
	static boolean safeScrollUntilVisibleByCoordinate(TestObject to, int startY, int endY, int maxSwipes = 20, int startX = -1, int endX = -1) {
		KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Mulai: target=${to?.getObjectId()} | startX=${startX} | startY=${startY} | endX=${endX} | endY=${endY} | maxSwipes=${maxSwipes}")
		if (to == null) {
			KeywordUtil.markFailed("[safeScrollUntilVisibleByCoordinate] TestObject tidak boleh null")
			return false
		}

		if (startX < 0 || endX < 0) {
			int windowWidth = safeGetWindowWidth()
			if (windowWidth <= 0) windowWidth = ((Number) WebUI.executeJavaScript("return window.innerWidth", null)).intValue()
			startX = Math.round(windowWidth / 2)
			endX = startX
			KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] X tidak diberikan, pakai centerX=${startX}")
		}

		String arahScroll = (startY >= endY) ? "ke atas" : "ke bawah"
		KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Arah scroll: ${arahScroll}")

		if (safeVerifyElementPresent(to, 1, 1)) {
			KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Objek sudah terlihat tanpa perlu scroll")
			return true
		}

		for (int attempt = 1; attempt <= maxSwipes; attempt++) {
			KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Scroll ${arahScroll} attempt ${attempt}: (${startX},${startY}) -> (${endX},${endY})")
			safeScrollBy(startX, startY, endX, endY)

			if (safeVerifyElementPresent(to, 1, 1)) {
				KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Objek ditemukan pada attempt ${attempt}")
				return true
			}
		}

		KeywordUtil.logInfo("[safeScrollUntilVisibleByCoordinate] Objek belum ditemukan setelah ${maxSwipes}x scroll")
		return false
	}

	// ======================== SAFE BACK ==============================
	/**
	 * Navigasi browser ke halaman sebelumnya (tombol Back) dengan retry.
	 * Jika gagal, hanya markWarning — test TIDAK berhenti.
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeBack(int retryCount = DEFAULT_RETRY) {
		boolean navigatedBack = withRetry('SafeBack', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeBack] Tekan tombol back | attempt=${attempt}/${retryCount}")
			WebUI.back()
			KeywordUtil.logInfo("[SafeBack] Berhasil back pada attempt ${attempt}")
			true
		}
		if (!navigatedBack) {
			KeywordUtil.markWarning("[SafeBack] Gagal pressBack setelah ${retryCount}x retry")
			KeywordUtil.logInfo("[SafeBack] STATUS: FAILED setelah ${retryCount}x retry")
		}
		return navigatedBack
	}

	// ======================== SAFE ENCRYPTED TEXT ==============================
	/**
	 * Isi field input menggunakan teks terenkripsi Katalon (dari GlobalVariable terenkripsi).
	 * Katalon akan otomatis mendekripsi nilai sebelum dimasukkan ke field.
	 * Cocok untuk input credential (password, API key) agar tidak tersimpan sebagai plaintext.
	 * @param encryptedText nilai terenkripsi dari Katalon Encrypt feature
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeSetEncryptedText(TestObject to, String encryptedText, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean encryptedTextSet = withRetry('SafeSetEncryptedText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSetEncryptedText] Mulai setEncryptedText: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			WebUI.setEncryptedText(to, encryptedText)
			KeywordUtil.logInfo("[SafeSetEncryptedText] Berhasil setEncryptedText: ${to?.getObjectId()} pada attempt ${attempt}")
			true
		}
		if (!encryptedTextSet) {
			KeywordUtil.markFailed("[SafeSetEncryptedText] Gagal setEncryptedText setelah ${retryCount}x retry: ${to?.getObjectId()}")
		}
		return encryptedTextSet
	}

	/**
	 * Isi field dengan teks terenkripsi — lempar StepFailedException jika gagal.
	 */
	static void safeSetEncryptedTextOrFail(TestObject to, String encryptedText, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[SafeSetEncryptedTextOrFail] Eksekusi: to=${to?.getObjectId()} | timeout=${timeout} | retry=${retryCount}")
		if (!safeSetEncryptedText(to, encryptedText, timeout, retryCount)) {
			KeywordUtil.logInfo("[SafeSetEncryptedTextOrFail] Gagal, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[SafeSetEncryptedTextOrFail] SetEncryptedText gagal: ${to.getObjectId()}")
		}
		KeywordUtil.logInfo("[SafeSetEncryptedTextOrFail] Sukses setEncryptedText: ${to?.getObjectId()}")
	}

	// ======================== SAFE VERIFY ELEMENT PRESENT ==============================
	/**
	 * Verifikasi elemen ada di halaman, dengan retry.
	 * Berbeda dengan safeWaitForElementPresent: method ini menggunakan waitForElementPresent
	 * langsung sebagai true/false check tanpa side-effect.
	 * @return true jika elemen hadir, false jika tidak
	 */
	static boolean safeVerifyElementPresent(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean present = withRetry('safeVerifyElementPresent', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeVerifyElementPresent] Verifikasi present: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!WebUI.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)) {
				throw new Exception('Element tidak present')
			}
			KeywordUtil.logInfo("[safeVerifyElementPresent] Hasil verify (attempt ${attempt}): present=true")
			true
		}
		if (!present) {
			KeywordUtil.logInfo("[safeVerifyElementPresent] STATUS: NOT PRESENT setelah ${retryCount}x attempt untuk: ${to?.getObjectId()}")
		}
		return present
	}

	/**
	 * Verifikasi elemen ada — lempar StepFailedException jika tidak ditemukan.
	 */
	static void safeVerifyElementPresentOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[safeVerifyElementPresentOrFail] Eksekusi: to=${to?.getObjectId()} | timeout=${timeout} | retry=${retryCount}")
		if (!safeVerifyElementPresent(to, timeout, retryCount)) {
			KeywordUtil.logInfo("[safeVerifyElementPresentOrFail] Tidak present, melempar exception: ${to?.getObjectId()}")
			throw new StepFailedException("[safeVerifyElementPresentOrFail] Element tidak ditemukan: ${to.getObjectId()}")
		}
		KeywordUtil.logInfo("[safeVerifyElementPresentOrFail] Element present: ${to?.getObjectId()}")
	}

	// ======================== SAFE VERIFY ELEMENT NOT PRESENT ==============================
	/**
	 * Verifikasi elemen TIDAK ada di halaman, dengan retry.
	 * Berguna untuk memastikan loading indicator / modal sudah hilang sebelum lanjut.
	 * @return true jika elemen sudah tidak ada, false jika masih ada
	 */
	static boolean safeVerifyElementNotPresent(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean notPresent = withRetry('safeVerifyElementNotPresent', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeVerifyElementNotPresent] Verifikasi not present: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!WebUI.waitForElementNotPresent(to, timeout, FailureHandling.OPTIONAL)) {
				throw new Exception('Element masih present')
			}
			KeywordUtil.logInfo("[safeVerifyElementNotPresent] Hasil verify (attempt ${attempt}): notPresent=true")
			true
		}
		if (!notPresent) {
			KeywordUtil.logInfo("[safeVerifyElementNotPresent] STATUS: STILL PRESENT setelah ${retryCount}x attempt untuk: ${to?.getObjectId()}")
		}
		return notPresent
	}

	/**
	 * Verifikasi elemen TIDAK ada di halaman — markFailed jika elemen masih ada (verifikasi gagal).
	 * Gunakan saat elemen wajib sudah hilang (misal: loading spinner, modal) untuk melanjutkan test.
	 */
	static void safeVerifyElementNotPresentOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		KeywordUtil.logInfo("[safeVerifyElementNotPresentOrFail] Eksekusi: to=${to?.getObjectId()} | timeout=${timeout} | retry=${retryCount}")
		if (!safeVerifyElementNotPresent(to, timeout, retryCount)) {
			KeywordUtil.logInfo("[safeVerifyElementNotPresentOrFail] Elemen masih ada, melempar exception: ${to?.getObjectId()}")
			KeywordUtil.markFailed("[safeVerifyElementNotPresentOrFail] Element masih ditemukan padahal seharusnya tidak ada: ${to.getObjectId()}")
			throw new StepFailedException("[safeVerifyElementNotPresentOrFail] Element masih ditemukan: ${to.getObjectId()}")
		}
		KeywordUtil.logInfo("[safeVerifyElementNotPresentOrFail] Element sudah tidak ada (not present): ${to?.getObjectId()}")
	}

	// ======================== SAFE GET WINDOW HEIGHT ==============================
	/**
	 * Ambil tinggi viewport browser saat ini (window.innerHeight) via JavaScript.
	 * Digunakan untuk menghitung koordinat scroll secara dinamis.
	 * @return tinggi window dalam pixel, atau -1 jika gagal
	 */
	static int safeGetWindowHeight(int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetWindowHeight', -1, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetWindowHeight] Ambil window innerHeight | attempt=${attempt}/${retryCount}")
			int h = ((Number) WebUI.executeJavaScript("return window.innerHeight", null)).intValue()
			KeywordUtil.logInfo("[safeGetWindowHeight] innerHeight=${h} pada attempt ${attempt}")
			h
		}
	}

	// ======================== SAFE GET WINDOW WIDTH ==============================
	/**
	 * Ambil lebar viewport browser saat ini (window.innerWidth) via JavaScript.
	 * Digunakan untuk menghitung koordinat tengah layar saat scroll.
	 * @return lebar window dalam pixel, atau -1 jika gagal
	 */
	static int safeGetWindowWidth(int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetWindowWidth', -1, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetWindowWidth] Ambil window innerWidth | attempt=${attempt}/${retryCount}")
			int w = ((Number) WebUI.executeJavaScript("return window.innerWidth", null)).intValue()
			KeywordUtil.logInfo("[safeGetWindowWidth] innerWidth=${w} pada attempt ${attempt}")
			w
		}
	}

	// ======================== SAFE GET ELEMENT LEFT POSITION ==============================
	/**
	 * Ambil posisi kiri (koordinat X) elemen relatif terhadap dokumen.
	 * Berguna untuk menghitung titik tengah elemen saat perlu tap/scroll ke posisi tertentu.
	 * @return posisi X dalam pixel, atau -1 jika gagal
	 */
	static int safeGetElementLeftPosition(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetElementLeftPosition', -1, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetElementLeftPosition] Ambil leftPosition: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			int pos = WebUI.getElementLeftPosition(to)
			KeywordUtil.logInfo("[safeGetElementLeftPosition] leftPosition=${pos} pada attempt ${attempt}")
			pos
		}
	}

	// ======================== SAFE GET ELEMENT TOP POSITION ==============================
	/**
	 * Ambil posisi atas (koordinat Y) elemen relatif terhadap dokumen.
	 * Berguna untuk membuat scroll spinner / scroll ke elemen di posisi Y tertentu.
	 * @return posisi Y dalam pixel, atau -1 jika gagal
	 */
	static int safeGetElementTopPosition(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetElementTopPosition', -1, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetElementTopPosition] Ambil topPosition: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			int pos = WebUI.getElementTopPosition(to)
			KeywordUtil.logInfo("[safeGetElementTopPosition] topPosition=${pos} pada attempt ${attempt}")
			pos
		}
	}

	// ======================== SAFE GET TEXT ==============================
	/**
	 * Ambil teks yang ditampilkan oleh elemen (innerText), dengan retry.
	 * @return teks elemen, atau null jika gagal setelah semua retry
	 */
	static String safeGetText(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetText', null, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetText] Ambil text: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			String val = WebUI.getText(to)
			KeywordUtil.logInfo("[safeGetText] Berhasil dapat text pada attempt ${attempt}: value='${val}'")
			val
		}
	}

	/**
	 * Ambil nilai atribut HTML elemen (misal: "value", "class", "href", "data-id"), dengan retry.
	 * @param attribute nama atribut HTML yang akan dibaca
	 * @return nilai atribut, atau null jika gagal setelah semua retry
	 */
	static String safeGetAttribute(TestObject to, String attribute, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('safeGetAttribute', null, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeGetAttribute] Ambil attribute '${attribute}': ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			String val = WebUI.getAttribute(to, attribute)
			KeywordUtil.logInfo("[safeGetAttribute] Berhasil dapat attribute '${attribute}' pada attempt ${attempt}: value='${val}'")
			val
		}
	}

	// ======================== SAFE SCROLL TO TEXT ==============================
	/**
	 * Scroll halaman sampai teks tertentu terlihat di viewport, dengan retry.
	 * Menggunakan JavaScript untuk mencari elemen text node exact-match,
	 * lalu memanggil scrollIntoView() pada elemen tersebut.
	 * @param text teks yang akan dicari dan di-scroll ke tampilan
	 * @return true jika berhasil, false jika gagal
	 */
	static boolean safeScrollToText(String text, int retryCount = DEFAULT_RETRY) {
		boolean scrolled = withRetry('safeScrollToText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[safeScrollToText] Mulai scrollToText '${text}' | attempt=${attempt}/${retryCount}")
			WebUI.executeJavaScript(
					"var els=document.querySelectorAll('*');for(var i=0;i<els.length;i++){if(els[i].childNodes.length===1&&els[i].childNodes[0].nodeType===3&&els[i].textContent.trim()===arguments[0]){els[i].scrollIntoView();break;}}",
					[text]
					)
			KeywordUtil.logInfo("[safeScrollToText] Berhasil scrollToText '${text}' pada attempt ${attempt}")
			true
		}
		if (!scrolled) {
			KeywordUtil.logInfo("[safeScrollToText] STATUS: FAILED scrollToText '${text}' setelah ${retryCount}x retry")
		}
		return scrolled
	}
}