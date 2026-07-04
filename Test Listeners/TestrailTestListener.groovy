import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

import internal.GlobalVariable
import hooks.TestrailIntegration

import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext

class TestrailTestListener {
	private static final Set<String> ALLOWED_PROFILES = ['default'].toSet()
	
	String executionProfile = ""
	String testsuitesName = ""
	String config = null
	
	@BeforeTestSuite
	def actionBeforeTestsuite(TestSuiteContext testSuiteContext) {
		executionProfile = RunConfiguration.getExecutionProfile()
		String currentSuiteId = testSuiteContext.getTestSuiteId()

		// Try to find if this TS is part of a TSC by scanning TSC XML files
		String tscName = findTscName(currentSuiteId)
		String nameClean = (tscName != null) ? tscName : currentSuiteId.tokenize('/').last()

		String gVarTestrunName = GlobalVariable.TESTRUN_NAME
		
		if(gVarTestrunName != null && !gVarTestrunName.trim().isEmpty()) {
			testsuitesName = GlobalVariable.TESTRUN_NAME
		}else {
			int index = nameClean.toLowerCase().indexOf("config")
			if (index != -1) {
				testsuitesName = nameClean.substring(0, index).trim()
				config = nameClean.substring(index + "config".length()).trim()
			} else {
				testsuitesName = nameClean.trim()
			}
		}
	}

	private String findTscName(String testSuiteId) {
		File tsSuiteDir = new File(RunConfiguration.getProjectDir(), "Test Suites")
		String tscName = null
		tsSuiteDir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
			if (tscName != null) return
			if (!file.name.endsWith('.ts')) return
			try {
				String text = file.text
				if (!text.contains('TestSuiteCollectionEntity')) return
				def xml = new XmlSlurper().parseText(text)
				xml.testSuiteRunConfigurations.TestSuiteRunConfiguration.each { entry ->
					if (entry.testSuiteEntity.text() == testSuiteId) {
						tscName = xml.name.text()
					}
				}
			} catch (ignored) { }
		}
		return tscName
	}
	
	@AfterTestCase
	def afterTestCase(TestCaseContext testcaseContext) {
		if (!GlobalVariable.TESTRAIL_INTEGRATION) return
		if (!ALLOWED_PROFILES.contains(executionProfile)) return

		def testcaseProperty = testcaseContext.getTestCaseVariables().get("testcaseProperties")
		if (testcaseProperty == null) return
		
		TestrailIntegration.addTestrunResult(testcaseProperty, testsuitesName, config)
	}
}