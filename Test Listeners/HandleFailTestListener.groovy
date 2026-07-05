import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject

import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

import internal.GlobalVariable as GlobalVariable

import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext

class HandleFailTestListener {
 
    /**
     * Mengecek kondisi sebelum Test Case berjalan.
     * Jika STOP_SUITE bernilai true, maka test case ini akan di-skip (dilewati).
     */
    @BeforeTestCase
    def sampleBeforeTestCase(TestCaseContext testCaseContext) {
        if (GlobalVariable.STOP_SUITE == true) {
            // Melewati (skip) test case yang belum tereksekusi
            testCaseContext.skipThisTestCase() 
        }
    }
 
    /**
     * Mengecek status setelah Test Case selesai.
     * Jika statusnya FAILED atau ERROR, ubah nilai STOP_SUITE menjadi true.
     */
    @AfterTestCase
    def sampleAfterTestCase(TestCaseContext testCaseContext) {
        def status = testCaseContext.getTestCaseStatus()
        
        if (status == 'FAILED' || status == 'ERROR') {
            GlobalVariable.STOP_SUITE = true
        }
    }
}
