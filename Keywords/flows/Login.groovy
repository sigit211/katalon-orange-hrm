package flows

import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCase
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows

import core.CredentialLoader
import core.SafeActionsWeb
import internal.GlobalVariable
import pages.LoginPage

public class Login {
	static Map<String, String> loginStep(String testcaseName){
		WebUI.comment("Starting loginStep for testcase: " + testcaseName)
		Map<String, String> userInfo = CredentialLoader.load("USER_INFO")

		SafeActionsWeb.safeSendKeys(LoginPage.inputUsername().testObject, userInfo.get("USERNAME"))
		SafeActionsWeb.safeSendKeys(LoginPage.inputPassword().testObject, userInfo.get("PASSWORD"))
		SafeActionsWeb.safeTakeScreenshot(testcaseName + "/input username and password.png")
		SafeActionsWeb.safeClick(LoginPage.buttonLogin().testObject)

		WebUI.delay(2)
		SafeActionsWeb.safeTakeScreenshot(testcaseName + "/after klik button login.png")
		
		String currentUrl = WebUI.getUrl()
		WebUI.verifyEqual(currentUrl.contains('/auth/login'), false)

		WebUI.comment("Finished loginStep for testcase: " + testcaseName + ". Current URL: " + currentUrl)
		return userInfo
	}
}
