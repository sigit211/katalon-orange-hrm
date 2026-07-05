import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext

import core.GlobalVariableConfig
import internal.GlobalVariable

/**
 * Listener untuk menerapkan override konfigurasi dari local.properties sebelum test dijalankan.
 * Berlaku baik saat run langsung dari Test Case maupun dari Test Suite.
 * File local.properties di-gitignore dan hanya ada di lokal masing-masing developer.
 */
class GlobalVariableConfigListener {

	private static boolean applied = false

	private static synchronized void applyLocalProperties() {
		if (applied) return
		Map<String, String> local = GlobalVariableConfig.loadLocalProperties()
		if (local.containsKey('BASE_URL')) GlobalVariable.BASE_URL = local.get('BASE_URL')
			
		// CATATAN: GlobalVariable.USER_ADMIN (tipe Map) TIDAK di-set di sini.
		// Katalon tidak menjamin Map yang di-assign dari listener terbaca oleh keyword
		// karena perbedaan Groovy classloader. Credential dibaca langsung via
		// CredentialLoader.load() di dalam keyword yang membutuhkannya.
		applied = true
	}

	@BeforeTestCase
	def beforeTestCase(TestCaseContext testCaseContext) {
		applyLocalProperties()
	}

	@BeforeTestSuite
	def beforeTestSuite(TestSuiteContext testSuiteContext) {
		applyLocalProperties()
	}
}