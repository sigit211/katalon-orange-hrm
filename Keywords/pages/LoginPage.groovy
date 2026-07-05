package pages

import core.TestObjectHelper
import core.TestObjectHelper.ObjectResult

public class LoginPage {
	static ObjectResult inputCsrfToken() {
		TestObjectHelper.testObject("name", "_token")
	}

	static ObjectResult inputUsername() {
		TestObjectHelper.testObject("name", "username")
	}

	static ObjectResult inputPassword() {
		TestObjectHelper.testObject("name", "password")
	}

	static ObjectResult buttonLogin() {
		TestObjectHelper.testObject("xpath", "//button[@type='submit' and normalize-space()='Login']")
	}

	static ObjectResult linkForgotPassword() {
		TestObjectHelper.testObject("xpath", "//a[normalize-space()='Forgot your password?']")
	}

	static ObjectResult linkLinkedIn() {
		TestObjectHelper.testObject("href", "https://www.linkedin.com/company/orangehrm/mycompany/")
	}

	static ObjectResult linkFacebook() {
		TestObjectHelper.testObject("href", "https://www.facebook.com/OrangeHRM/")
	}

	static ObjectResult linkTwitter() {
		TestObjectHelper.testObject("href", "https://twitter.com/orangehrm?lang=en")
	}

	static ObjectResult linkYouTube() {
		TestObjectHelper.testObject("href", "https://www.youtube.com/c/OrangeHRMInc")
	}

	static ObjectResult imageBranding() {
		TestObjectHelper.testObject("alt", "company-branding")
	}

	static ObjectResult imageLogo() {
		TestObjectHelper.testObject("alt", "orangehrm-logo")
	}
}
