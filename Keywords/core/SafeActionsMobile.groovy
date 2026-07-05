package core

import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil

import internal.GlobalVariable

/**
 * SafeActionsMobile
 *
 * Kumpulan method aksi Mobile yang dilengkapi mekanisme retry otomatis
 * dan logging detail. Setiap method dibagi menjadi dua varian:
 *   - boolean  : mengembalikan true/false, tidak melempar exception.
 *   - OrFail   : melempar StepFailedException jika aksi gagal setelah semua retry.
 *
 * Konstanta default:
 *   DEFAULT_RETRY    = jumlah percobaan ulang maksimum
 *   DEFAULT_DELAY    = jeda antar retry (detik)
 *   DEFAULT_TIMEOUT  = timeout tunggu element (detik, dari GlobalVariable)
 *   MAX_WAIT_SECONDS = batas maksimum menunggu element (detik, dari GlobalVariable)
 */
public class SafeActionsMobile {
	static int DEFAULT_RETRY = 3
	static int DEFAULT_DELAY = 1
	static int DEFAULT_TIMEOUT = GlobalVariable.LONG_TIMEOUT
	static int MAX_WAIT_SECONDS = GlobalVariable.MAX_WAIT_SECONDS

	// ======================== INTERNAL RETRY HELPER ==============================
	/**
	 * Eksekusi aksi dengan mekanisme retry otomatis.
	 * Jika aksi sukses (closure tidak melempar exception), hasilnya langsung dikembalikan.
	 * Jika gagal, dicatat ke log dan dicoba ulang hingga retryCount kali.
	 * @param tag         Nama method untuk keperluan logging.
	 * @param failValue   Nilai yang dikembalikan jika semua retry habis.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @param action      Closure yang menerima nomor attempt (1-based).
	 * @return Hasil dari action jika berhasil, atau failValue jika gagal.
	 */
	private static def withRetry(String tag, def failValue, int retryCount, Closure action) {
		for (int attempt = 1; attempt <= retryCount; attempt++) {
			try {
				return action.call(attempt)
			} catch (Exception e) {
				KeywordUtil.logInfo("[${tag}] Gagal attempt ${attempt}/${retryCount} | error='${e.message}'")
				if (attempt < retryCount) Mobile.delay(DEFAULT_DELAY)
			}
		}
		return failValue
	}

	// ======================== SAFE TAP ==============================
	/**
	 * Mencoba melakukan tap pada element. Akan retry sebanyak retryCount jika gagal.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika tap berhasil, false jika semua retry habis.
	 */
	static boolean safeTap(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeTap', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeTap] Tap: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.tap(to, timeout)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeTap] Gagal tap setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeTapOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeTap(to, timeout, retryCount))
			throw new StepFailedException("[SafeTapOrFail] Tap gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE TAP AND HOLD ==============================
	/**
	 * Mencoba melakukan tap dan tahan (long press) pada element.
	 * @param to          TestObject target.
	 * @param duration    Durasi tahan dalam milidetik.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeTapAndHold(TestObject to, int duration = 2000, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeTapAndHold', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeTapAndHold] TapAndHold: ${to?.getObjectId()} | duration=${duration}ms | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.tapAndHold(to, duration, timeout)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeTapAndHold] Gagal tapAndHold setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeTapAndHoldOrFail(TestObject to, int duration = 2000, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeTapAndHold(to, duration, timeout, retryCount))
			throw new StepFailedException("[SafeTapAndHoldOrFail] TapAndHold gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE DOUBLE TAP ==============================
	/**
	 * Mencoba melakukan double tap pada element.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeDoubleTap(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeDoubleTap', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeDoubleTap] DoubleTap: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.doubleTap(to, timeout)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeDoubleTap] Gagal doubleTap setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeDoubleTapOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeDoubleTap(to, timeout, retryCount))
			throw new StepFailedException("[SafeDoubleTapOrFail] DoubleTap gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE TAP AT POSITION ==============================
	/**
	 * Mencoba melakukan tap pada koordinat layar (x, y).
	 * @param x           Koordinat X pada layar.
	 * @param y           Koordinat Y pada layar.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeTapAtPosition(int x, int y, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeTapAtPosition', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeTapAtPosition] TapAtPosition (${x}, ${y}) | attempt=${attempt}/${retryCount}")
			Mobile.tapAtPosition(x, y)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeTapAtPosition] Gagal setelah ${retryCount}x retry: (${x}, ${y})")
		return ok
	}

	static void safeTapAtPositionOrFail(int x, int y, int retryCount = DEFAULT_RETRY) {
		if (!safeTapAtPosition(x, y, retryCount))
			throw new StepFailedException("[SafeTapAtPositionOrFail] TapAtPosition gagal: (${x}, ${y})")
	}

	// ======================== SAFE SET TEXT ==========================
	/**
	 * Mencoba mengisi teks pada element.
	 * @param to          TestObject target.
	 * @param text        Teks yang akan dimasukkan.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeSetText(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeSetText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSetText] SetText: ${to?.getObjectId()} | text='${text}' | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.setText(to, text, timeout)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeSetText] Gagal setText setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeSetTextOrFail(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeSetText(to, text, timeout, retryCount))
			throw new StepFailedException("[SafeSetTextOrFail] SetText gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE SEND KEYS =========================
	/**
	 * Mencoba mengirim keystroke/teks pada element menggunakan Mobile.sendKeys.
	 * Berbeda dengan safeSetText: cocok untuk input khusus/keyboard action.
	 * @param to          TestObject target.
	 * @param text        Teks/key yang akan dikirim.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeSendKeys(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeSendKeys', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSendKeys] SendKeys: ${to?.getObjectId()} | text='${text}' | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.sendKeys(to, text)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeSendKeys] Gagal sendKeys setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeSendKeysOrFail(TestObject to, String text, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeSendKeys(to, text, timeout, retryCount))
			throw new StepFailedException("[SafeSendKeysOrFail] SendKeys gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE CLEAR TEXT =========================
	/**
	 * Mencoba menghapus teks pada element.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu element present (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeClearText(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeClearText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeClearText] ClearText: ${to?.getObjectId()} | attempt=${attempt}/${retryCount}")
			Mobile.waitForElementPresent(to, timeout)
			Mobile.clearText(to, timeout)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeClearText] Gagal clearText setelah ${retryCount}x retry: ${to?.getObjectId()}")
		return ok
	}

	static void safeClearTextOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeClearText(to, timeout, retryCount))
			throw new StepFailedException("[SafeClearTextOrFail] ClearText gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE WAIT ==============================
	/**
	 * Menunggu element muncul (present) dengan retry. Tidak melempar exception jika tidak ditemukan.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu per percobaan (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element present, false jika tidak ditemukan setelah semua retry.
	 */
	static boolean safeWait(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeWait', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeWait] Menunggu: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!Mobile.waitForElementPresent(to, timeout)) throw new Exception("Element tidak present")
			true
		}
	}

	/**
	 * Menunggu element exist dengan retry. Melempar StepFailedException jika tidak ditemukan.
	 */
	static void safeWaitElementOrFail(TestObject to, int retryCount, int maxWaitSeconds = MAX_WAIT_SECONDS) {
		if (!safeVerifyElementExist(to, maxWaitSeconds, retryCount))
			throw new StepFailedException("[SafeWaitElementOrFail] Object '${to?.getObjectId()}' tidak ditemukan dalam ${maxWaitSeconds * retryCount} detik!")
	}

	// ======================== SAFE WAIT FOR ELEMENT NOT PRESENT ==============================
	/**
	 * Menunggu element menghilang (not present). Berguna untuk menunggu loading spinner hilang.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu per percobaan (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element sudah tidak present, false jika masih ada setelah semua retry.
	 */
	static boolean safeWaitForElementNotPresent(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeWaitForElementNotPresent', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeWaitForElementNotPresent] Menunggu hilang: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!Mobile.waitForElementNotPresent(to, timeout)) throw new Exception("Element masih present")
			true
		}
	}

	static void safeWaitForElementNotPresentOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeWaitForElementNotPresent(to, timeout, retryCount))
			throw new StepFailedException("[SafeWaitForElementNotPresentOrFail] Element masih present: ${to?.getObjectId()}")
	}

	// ======================== SAFE SCREENSHOT ========================
	/**
	 * Mengambil screenshot ke path yang ditentukan. Kegagalan dicatat sebagai warning (non-blocking).
	 * Menggunakan progressive delay: attempt ke-N menunggu N detik sebelum screenshot.
	 * @param path        Path file tujuan screenshot.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeScreenshot(String path, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeScreenshot', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeScreenshot] Screenshot ke: '${path}' | attempt=${attempt}/${retryCount}")
			Mobile.delay(attempt)
			Mobile.takeScreenshot(path)
			true
		}
		if (!ok) KeywordUtil.markWarning("[SafeScreenshot] Screenshot gagal setelah ${retryCount}x retry ke '${path}'")
		return ok
	}

	// ======================== SAFE SWIPE =============================
	/**
	 * Melakukan swipe dari koordinat awal ke koordinat akhir.
	 * @param startX, startY  Koordinat awal.
	 * @param endX, endY      Koordinat tujuan.
	 * @param retryCount      Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeSwipe(int startX, int startY, int endX, int endY, int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeSwipe', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSwipe] Swipe (${startX},${startY}) -> (${endX},${endY}) | attempt=${attempt}/${retryCount}")
			Mobile.swipe(startX, startY, endX, endY)
			true
		}
		if (!ok) KeywordUtil.markFailed("[SafeSwipe] Gagal swipe setelah ${retryCount}x retry")
		return ok
	}

	static void safeSwipeOrFail(int startX, int startY, int endX, int endY, int retryCount = DEFAULT_RETRY) {
		if (!safeSwipe(startX, startY, endX, endY, retryCount))
			throw new StepFailedException("[SafeSwipeOrFail] Swipe gagal (${startX},${startY}) -> (${endX},${endY})")
	}

	// ======================== SAFE SWIPE UNTIL VISIBLE =============================
	/**
	 * Melakukan swipe berulang sampai element target terlihat di layar.
	 * Arah swipe ditentukan otomatis: startY < endY → ke atas, startY > endY → ke bawah.
	 * @param to         TestObject yang dicari.
	 * @param startY     Koordinat Y awal swipe.
	 * @param endY       Koordinat Y akhir swipe.
	 * @param maxSwipes  Jumlah maksimum swipe.
	 * @return true jika element ditemukan, false jika maxSwipes tercapai.
	 */
	static boolean safeSwipeUntilVisible(TestObject to, int startY, int endY, int maxSwipes = 20) {
		KeywordUtil.logInfo("[SafeSwipeUntilVisible] Mulai: ${to?.getObjectId()} | startY=${startY} | endY=${endY} | maxSwipes=${maxSwipes}")
		if (to == null) {
			KeywordUtil.markFailed("[SafeSwipeUntilVisible] TestObject tidak boleh null")
			return false
		}
		if (safeVerifyElementExist(to, 1, 1)) {
			KeywordUtil.logInfo("[SafeSwipeUntilVisible] Objek sudah terlihat tanpa perlu swipe")
			return true
		}
		int deviceWidth = safeGetDeviceWidth()
		if (deviceWidth <= 0) deviceWidth = Mobile.getDeviceWidth()
		int centerX = Math.round(deviceWidth / 2)
		String arahSwipe = startY <= endY ? "ke atas" : "ke bawah"
		KeywordUtil.logInfo("[SafeSwipeUntilVisible] centerX=${centerX} | arah=${arahSwipe}")

		for (int attempt = 1; attempt <= maxSwipes; attempt++) {
			safeSwipe(centerX, startY, centerX, endY)
			if (safeVerifyElementExist(to, 1, 1)) {
				KeywordUtil.logInfo("[SafeSwipeUntilVisible] Objek ditemukan pada swipe attempt ${attempt}")
				return true
			}
		}
		KeywordUtil.logInfo("[SafeSwipeUntilVisible] Objek belum ditemukan setelah ${maxSwipes}x swipe")
		return false
	}

	static void safeSwipeUntilVisibleOrFail(TestObject to, int startY, int endY, int maxSwipes = 20) {
		if (!safeSwipeUntilVisible(to, startY, endY, maxSwipes))
			throw new StepFailedException("[SafeSwipeUntilVisibleOrFail] Element tidak ditemukan setelah ${maxSwipes}x swipe: ${to?.getObjectId()}")
	}

	// ======================== SAFE BACK ==============================
	/** Menekan tombol back pada device. */
	static boolean safeBack(int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeBack', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeBack] Tekan back | attempt=${attempt}/${retryCount}")
			Mobile.pressBack()
			true
		}
		if (!ok) KeywordUtil.markWarning("[SafeBack] Gagal pressBack setelah ${retryCount}x retry")
		return ok
	}

	static void safeBackOrFail(int retryCount = DEFAULT_RETRY) {
		if (!safeBack(retryCount))
			throw new StepFailedException("[SafeBackOrFail] PressBack gagal setelah ${retryCount}x retry")
	}

	// ======================== SAFE PRESS HOME ==============================
	/** Menekan tombol home pada device untuk kembali ke layar utama. */
	static boolean safePressHome(int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafePressHome', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafePressHome] Tekan home | attempt=${attempt}/${retryCount}")
			Mobile.pressHome()
			true
		}
		if (!ok) KeywordUtil.markWarning("[SafePressHome] Gagal pressHome setelah ${retryCount}x retry")
		return ok
	}

	static void safePressHomeOrFail(int retryCount = DEFAULT_RETRY) {
		if (!safePressHome(retryCount))
			throw new StepFailedException("[SafePressHomeOrFail] PressHome gagal setelah ${retryCount}x retry")
	}

	// ======================== SAFE HIDE KEYBOARD ==============================
	/** Menyembunyikan keyboard yang sedang muncul. */
	static boolean safeHideKeyboard(int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeHideKeyboard', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeHideKeyboard] HideKeyboard | attempt=${attempt}/${retryCount}")
			Mobile.hideKeyboard()
			true
		}
		if (!ok) KeywordUtil.markWarning("[SafeHideKeyboard] Gagal hideKeyboard setelah ${retryCount}x retry")
		return ok
	}

	static void safeHideKeyboardOrFail(int retryCount = DEFAULT_RETRY) {
		if (!safeHideKeyboard(retryCount))
			throw new StepFailedException("[SafeHideKeyboardOrFail] HideKeyboard gagal setelah ${retryCount}x retry")
	}

	// ======================== SAFE ENCRYPTED TEXT ==============================
	/**
	 * Mengisi field dengan teks terenkripsi (Katalon encrypted text).
	 * @param to             TestObject target.
	 * @param encryptedText  Teks terenkripsi (format Katalon).
	 * @param timeout        Timeout untuk setEncryptedText (detik).
	 * @param retryCount     Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeSetEncryptedText(TestObject to, String encryptedText, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeSetEncryptedText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeSetEncryptedText] SetEncryptedText: ${to?.getObjectId()} | attempt=${attempt}/${retryCount}")
			Mobile.setEncryptedText(to, encryptedText, timeout)
			true
		}
	}

	static void safeSetEncryptedTextOrFail(TestObject to, String encryptedText, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeSetEncryptedText(to, encryptedText, timeout, retryCount))
			throw new StepFailedException("[SafeSetEncryptedTextOrFail] SetEncryptedText gagal: ${to?.getObjectId()}")
	}

	// ======================== SAFE VERIFY ELEMENT EXIST ==============================
	/**
	 * Memverifikasi bahwa element ada (exist) di halaman.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu per percobaan (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element exist, false jika tidak ditemukan setelah semua retry.
	 */
	static boolean safeVerifyElementExist(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeVerifyElementExist', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeVerifyElementExist] Verifikasi exist: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!Mobile.verifyElementExist(to, timeout)) throw new Exception("Element tidak exist")
			true
		}
	}

	static void safeVerifyElementExistOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeVerifyElementExist(to, timeout, retryCount))
			throw new StepFailedException("[SafeVerifyElementExistOrFail] Element tidak ditemukan: ${to?.getObjectId()}")
	}

	/**
	 * Cek keberadaan element menggunakan waitForElementPresent (tidak throw/mark test FAILED jika tidak ada).
	 * Cocok untuk kondisi opsional seperti cek dialog yang mungkin tidak selalu muncul.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu (detik).
	 * @return true jika element ditemukan, false jika tidak ada (tanpa error).
	 */
	static boolean safeIsElementPresent(TestObject to, int timeout = DEFAULT_TIMEOUT) {
		try {
			KeywordUtil.logInfo("[SafeIsElementPresent] Cek element: ${to?.getObjectId()} | timeout=${timeout}")
			return Mobile.waitForElementPresent(to, timeout)
		} catch (Throwable ignore) {
			return false
		}
	}

	/**
	 * Cek keberadaan element opsional secara senyap — khusus untuk popup/dialog yang belum pasti muncul.
	 * Menggunakan FailureHandling.OPTIONAL: jika element tidak ada, step tercatat sebagai skip (abu-abu),
	 * bukan warning (kuning) maupun failure (merah). Tidak ada tanda apapun di hasil test.
	 * @param to       TestObject target.
	 * @param timeout  Waktu tunggu dalam detik (default: 1).
	 * @return true jika element ditemukan, false jika tidak ada (tanpa warning).
	 */
	static boolean safeIsOptionalElementPresent(TestObject to, int timeout = DEFAULT_TIMEOUT) {
		KeywordUtil.logInfo("[SafeIsOptionalElementPresent] Cek element opsional: ${to?.getObjectId()} | timeout=${timeout}")
		boolean found = Mobile.waitForElementPresent(to, timeout, FailureHandling.OPTIONAL)
		KeywordUtil.logInfo("[SafeIsOptionalElementPresent] result=${found}")
		return found
	}

	// ======================== SAFE VERIFY ELEMENT NOT EXIST ==============================
	/**
	 * Memverifikasi bahwa element TIDAK ada (not exist) di halaman.
	 * @param to          TestObject target.
	 * @param timeout     Waktu tunggu per percobaan (detik).
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element not exist, false jika masih ada setelah semua retry.
	 */
	static boolean safeVerifyElementNotExist(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeVerifyElementNotExist', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeVerifyElementNotExist] Verifikasi not exist: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			if (!Mobile.verifyElementNotExist(to, timeout)) throw new Exception("Element masih exist")
			true
		}
	}

	static void safeVerifyElementNotExistOrFail(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		if (!safeVerifyElementNotExist(to, timeout, retryCount))
			throw new StepFailedException("[SafeVerifyElementNotExistOrFail] Element masih ada: ${to?.getObjectId()}")
	}

	// ======================== SAFE VERIFY ELEMENT VISIBLE ==============================
	/**
	 * Memverifikasi bahwa element terlihat (visible) di viewport layar.
	 * Berbeda dengan safeVerifyElementExist: element harus benar-benar tampil, tidak hanya ada di DOM.
	 * @param to          TestObject target.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element visible, false jika tidak visible setelah semua retry.
	 */
	static boolean safeVerifyElementVisible(TestObject to, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeVerifyElementVisible', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeVerifyElementVisible] Verifikasi visible: ${to?.getObjectId()} | attempt=${attempt}/${retryCount}")
			if (!Mobile.verifyElementVisible(to, DEFAULT_TIMEOUT)) throw new Exception("Element tidak visible")
			true
		}
	}

	static void safeVerifyElementVisibleOrFail(TestObject to, int retryCount = DEFAULT_RETRY) {
		if (!safeVerifyElementVisible(to, retryCount))
			throw new StepFailedException("[SafeVerifyElementVisibleOrFail] Element tidak visible: ${to?.getObjectId()}")
	}

	// ======================== SAFE VERIFY ELEMENT NOT VISIBLE ==============================
	/**
	 * Memverifikasi bahwa element TIDAK terlihat (not visible) di viewport layar.
	 * Berguna untuk memastikan overlay, modal, atau elemen lain sudah hilang dari viewport.
	 * @param to          TestObject target.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika element tidak visible, false jika masih visible setelah semua retry.
	 */
	static boolean safeVerifyElementNotVisible(TestObject to, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeVerifyElementNotVisible', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeVerifyElementNotVisible] Verifikasi not visible: ${to?.getObjectId()} | attempt=${attempt}/${retryCount}")
			if (!Mobile.verifyElementNotVisible(to, DEFAULT_TIMEOUT)) throw new Exception("Element masih visible")
			true
		}
	}

	static void safeVerifyElementNotVisibleOrFail(TestObject to, int retryCount = DEFAULT_RETRY) {
		if (!safeVerifyElementNotVisible(to, retryCount))
			throw new StepFailedException("[SafeVerifyElementNotVisibleOrFail] Element masih visible: ${to?.getObjectId()}")
	}

	// ======================== SAFE GET DEVICE DIMENSIONS ==============================
	/**
	 * Mengambil tinggi layar device.
	 * @return Tinggi device dalam pixel, atau -1 jika gagal.
	 */
	static int safeGetDeviceHeight(int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetDeviceHeight', -1, retryCount) { int attempt ->
			int h = Mobile.getDeviceHeight()
			KeywordUtil.logInfo("[SafeGetDeviceHeight] deviceHeight=${h}")
			h
		}
	}

	/**
	 * Mengambil lebar layar device.
	 * @return Lebar device dalam pixel, atau -1 jika gagal.
	 */
	static int safeGetDeviceWidth(int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetDeviceWidth', -1, retryCount) { int attempt ->
			int w = Mobile.getDeviceWidth()
			KeywordUtil.logInfo("[SafeGetDeviceWidth] deviceWidth=${w}")
			w
		}
	}

	// ======================== SAFE GET ELEMENT POSITION & SIZE ==============================
	/**
	 * Mengambil posisi/ukuran element. Semua mengembalikan nilai pixel, atau -1 jika gagal.
	 */
	static int safeGetElementLeftPosition(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetElementLeftPosition', -1, retryCount) { int attempt ->
			int pos = Mobile.getElementLeftPosition(to, timeout)
			KeywordUtil.logInfo("[SafeGetElementLeftPosition] leftPosition=${pos} untuk ${to?.getObjectId()}")
			pos
		}
	}

	static int safeGetElementTopPosition(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetElementTopPosition', -1, retryCount) { int attempt ->
			int pos = Mobile.getElementTopPosition(to, timeout)
			KeywordUtil.logInfo("[SafeGetElementTopPosition] topPosition=${pos} untuk ${to?.getObjectId()}")
			pos
		}
	}

	static int safeGetElementWidth(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetElementWidth', -1, retryCount) { int attempt ->
			int width = Mobile.getElementWidth(to, timeout)
			KeywordUtil.logInfo("[SafeGetElementWidth] width=${width} untuk ${to?.getObjectId()}")
			width
		}
	}

	static int safeGetElementHeight(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetElementHeight', -1, retryCount) { int attempt ->
			int height = Mobile.getElementHeight(to, timeout)
			KeywordUtil.logInfo("[SafeGetElementHeight] height=${height} untuk ${to?.getObjectId()}")
			height
		}
	}

	// ======================== SAFE GET TEXT ==============================
	/**
	 * Mengambil teks dari element.
	 * @return String teks element, atau null jika semua percobaan gagal.
	 */
	static String safeGetText(TestObject to, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetText', null, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeGetText] GetText: ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			String val = Mobile.getText(to, timeout)
			KeywordUtil.logInfo("[SafeGetText] text='${val}'")
			val
		}
	}

	// ======================== SAFE GET ATTRIBUTE ==============================
	/**
	 * Mengambil nilai attribute dari element.
	 * @param attribute  Nama attribute (misal: 'text', 'enabled', 'checked').
	 * @return String nilai attribute, atau null jika semua percobaan gagal.
	 */
	static String safeGetAttribute(TestObject to, String attribute, int timeout = DEFAULT_TIMEOUT, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeGetAttribute', null, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeGetAttribute] GetAttribute '${attribute}': ${to?.getObjectId()} | timeout=${timeout} | attempt=${attempt}/${retryCount}")
			String val = Mobile.getAttribute(to, attribute, timeout)
			KeywordUtil.logInfo("[SafeGetAttribute] '${attribute}'='${val}'")
			val
		}
	}

	// ======================== SAFE SCROLL TO TEXT ==============================
	/**
	 * Melakukan scroll hingga teks tertentu terlihat di layar.
	 * @param text        Teks yang dicari saat scroll.
	 * @param retryCount  Jumlah maksimum percobaan.
	 * @return true jika berhasil, false jika semua retry habis.
	 */
	static boolean safeScrollToText(String text, int retryCount = DEFAULT_RETRY) {
		return withRetry('SafeScrollToText', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeScrollToText] ScrollToText '${text}' | attempt=${attempt}/${retryCount}")
			Mobile.scrollToText(text)
			true
		}
	}

	static void safeScrollToTextOrFail(String text, int retryCount = DEFAULT_RETRY) {
		if (!safeScrollToText(text, retryCount))
			throw new StepFailedException("[SafeScrollToTextOrFail] ScrollToText gagal untuk teks: '${text}'")
	}

	// ======================== SAFE OPEN NOTIFICATIONS ==============================
	/** Membuka panel notifikasi pada device. */
	static boolean safeOpenNotifications(int retryCount = DEFAULT_RETRY) {
		boolean ok = withRetry('SafeOpenNotifications', false, retryCount) { int attempt ->
			KeywordUtil.logInfo("[SafeOpenNotifications] OpenNotifications | attempt=${attempt}/${retryCount}")
			Mobile.openNotifications()
			true
		}
		if (!ok) KeywordUtil.markWarning("[SafeOpenNotifications] Gagal openNotifications setelah ${retryCount}x retry")
		return ok
	}

	static void safeOpenNotificationsOrFail(int retryCount = DEFAULT_RETRY) {
		if (!safeOpenNotifications(retryCount))
			throw new StepFailedException("[SafeOpenNotificationsOrFail] OpenNotifications gagal setelah ${retryCount}x retry")
	}
}
