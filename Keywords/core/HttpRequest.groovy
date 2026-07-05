package core

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.impl.HttpTextBodyContent
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import internal.GlobalVariable

class HttpRequest {
	//================================================== POST ==================================================//
	/**
	 * POST dengan pilihan body:
	 * - form-urlencoded: kirim Map<String, String>
	 * - json: kirim Map/POJO/String JSON
	 *
	 * @param url endpoint
	 * @param body data form (Map) ATAU JSON (Map/String)
	 * @param isJson true jika body mau dikirim sebagai application/json
	 * @param headers header tambahan (mis. Authorization)
	 * @param cookie optional Cookie header
	 * @param expectedStatus status code yang diharapkan
	 *
	 *
	 // POST form-urlencoded
	 def formResp = ApiKeywords.postFlexible(
	 "https://api.example.com/login",
	 [username: "john doe", password: "abc@123"],
	 false, // isJson = false
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}"],
	 "",    // cookie
	 200
	 )
	 // POST raw JSON (Map akan di-serialize)
	 def jsonResp = ApiKeywords.postFlexible(
	 "https://api.example.com/items",
	 [name: "Sample", active: true],
	 true, // isJson = true
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}"],
	 "",
	 201
	 )
	 // POST raw JSON dengan String (sudah format JSON)
	 def jsonString = '{"name":"Sample","active":true}'
	 def jsonResp2 = ApiKeywords.postFlexible(
	 "https://api.example.com/items",
	 jsonString,
	 true,
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}", "Accept":"application/json"],
	 "",
	 201
	 )
	 */
	static def postRequest(String url, Object body, boolean isJson = false, Map<String, String> headers = [:], String cookie = '', int expectedStatus = 200) {
		KeywordUtil.logInfo("[HttpRequest.postRequest] START url=${url} isJson=${isJson} expectedStatus=${expectedStatus}")
		RequestObject req = new RequestObject("POST Flexible Request")
		req.setRestUrl(url)
		req.setRestRequestMethod('POST')

		// Setup headers
		List<TestObjectProperty> headerProps = []
		// Content-Type
		String contentType = isJson ? 'application/json' : 'application/x-www-form-urlencoded'
		headerProps.add(new TestObjectProperty('Content-Type', ConditionType.EQUALS, contentType))

		// Authorization & lainnya
		headers?.each { k, v ->
			headerProps.add(new TestObjectProperty(k, ConditionType.EQUALS, v))
		}

		// Cookie optional
		if (cookie) {
			headerProps.add(new TestObjectProperty('Cookie', ConditionType.EQUALS, cookie))
		}
		req.setHttpHeaderProperties(headerProps)

		// Build body
		String payload
		if (isJson) {
			// Jika body sudah String JSON -> kirim apa adanya, kalau Map -> serialize ke JSON
			if (body instanceof String) {
				payload = (String) body
			} else {
				payload = JsonOutput.toJson(body) // safe serialize
			}
		} else {
			// form-urlencoded: gunakan Map<String, String>
			if (!(body instanceof Map)) {
				KeywordUtil.markFailedAndStop("Untuk form-urlencoded, body harus Map<String, String>.")
			}
			Map<String, String> data = (Map<String, String>) body

			// Handle null -> ganti jadi kosong
			payload = data.collect { k, v ->
				def key = URLEncoder.encode(String.valueOf(k), 'UTF-8')
				def val = URLEncoder.encode(v == null ? '' : String.valueOf(v), 'UTF-8')
				"${key}=${val}"
			}.join('&')
		}


		req.setBodyContent(new HttpTextBodyContent(payload, "UTF-8", contentType))

		// Kirim request
		ResponseObject response = WS.sendRequest(req)

		// Validasi status code
		int actualStatus = response.getStatusCode()
		if (actualStatus != expectedStatus) {
			KeywordUtil.markFailedAndStop("Status code mismatch. Expected: ${expectedStatus}, Actual: ${actualStatus}")
		}

		// Parse response: coba JSON, kalau gagal kembalikan String
		String responseText = response.getResponseText()
		if (!responseText) {
			KeywordUtil.markWarning("Response body kosong.")
			return null
		}

		try {
			return new JsonSlurper().parseText(responseText)
		} catch (Exception e) {
			KeywordUtil.logInfo("Response bukan JSON valid. Kembalikan teks biasa.")
			return responseText
		}
	}

	@Keyword
	static def postMultipart(String urlStr, Map<String, String> headers = [:], String cookie = '', Map<String, String> fields = [:], Map<String, File> files = [:], int expectedStatus = 200) {
		String boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
		String lineEnd = "\r\n"
		String twoHyphens = "--"

		KeywordUtil.logInfo("==========[MULTIPART DEBUG LOG]==========")
		KeywordUtil.logInfo("Target URL       : ${urlStr}")
		KeywordUtil.logInfo("Headers (count)  : ${headers?.size() ?: 0}")
		KeywordUtil.logInfo("Cookie           : ${cookie ? 'Present' : 'Missing'}")
		KeywordUtil.logInfo("Fields (count)   : ${fields?.size() ?: 0}")
		KeywordUtil.logInfo("Files (count)    : ${files?.size() ?: 0}")
		KeywordUtil.logInfo("Expected Status  : ${expectedStatus}")
		KeywordUtil.logInfo("=========================================")

		HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection()
		connection.setRequestMethod("POST")
		connection.setDoOutput(true)

		// Default headers
		connection.setRequestProperty("User-Agent", "KatalonMultipart/1.0")
		connection.setRequestProperty("Accept", "application/json")
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)

		// Custom headers
		headers?.each { k, v ->
			if (k && v) connection.setRequestProperty(k, v)
		}
		// Cookie optional
		if (cookie) connection.setRequestProperty("Cookie", cookie)

		// Build multipart body using buffer
		ByteArrayOutputStream buffer = new ByteArrayOutputStream()
		DataOutputStream outputStream = new DataOutputStream(buffer)

		// Write fields (text parts)
		fields?.each { name, value ->
			outputStream.writeBytes(twoHyphens + boundary + lineEnd)
			outputStream.writeBytes("Content-Disposition: form-data; name=\"${name}\"" + lineEnd + lineEnd)
			outputStream.writeBytes(String.valueOf(value ?: "") + lineEnd)
		}

		// Write files (binary parts)
		files?.each { name, file ->
			if (file == null) return
				outputStream.writeBytes(twoHyphens + boundary + lineEnd)
			outputStream.writeBytes("Content-Disposition: form-data; name=\"${name}\"; filename=\"${file.getName()}\"" + lineEnd)
			// MIME type best effort (fallback ke application/octet-stream)
			String mime = "application/octet-stream"
			if (file.name.toLowerCase().endsWith(".csv")) mime = "text/csv"
			else if (file.name.toLowerCase().endsWith(".png")) mime = "image/png"
			else if (file.name.toLowerCase().endsWith(".jpg") || file.name.toLowerCase().endsWith(".jpeg")) mime = "image/jpeg"

			outputStream.writeBytes("Content-Type: ${mime}" + lineEnd + lineEnd)

			file.withInputStream { inputStream ->
				byte[] buf = new byte[4096]
				int len
				while ((len = inputStream.read(buf)) != -1) {
					outputStream.write(buf, 0, len)
				}
			}
			outputStream.writeBytes(lineEnd)
		}

		// End boundary
		outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
		outputStream.flush()
		outputStream.close()

		// Send
		OutputStream out = connection.getOutputStream()
		out.write(buffer.toByteArray())
		out.flush()
		out.close()

		// Read response
		int code = connection.getResponseCode()
		InputStream respStream = (code >= 200 && code < 400) ? connection.getInputStream() : connection.getErrorStream()
		String resp = respStream?.getText("UTF-8") ?: ""

		KeywordUtil.logInfo("==========[SERVER RESPONSE]==========")
		KeywordUtil.logInfo("HTTP Code : ${code}")
		KeywordUtil.logInfo("Body      : ${resp}")
		KeywordUtil.logInfo("=====================================")

		// Status check
		if (code != expectedStatus) {
			KeywordUtil.markFailedAndStop("Status code mismatch. Expected: ${expectedStatus}, Actual: ${code}. Response: ${resp}")
		}

		if (!resp || resp.trim().isEmpty()) {
			throw new AssertionError("Response kosong dari server.")
		}

		// Parse JSON kalau bisa
		try {
			return new JsonSlurper().parseText(resp)
		} catch (Exception e) {
			KeywordUtil.logInfo("Response bukan JSON valid. Kembalikan teks biasa.")
			return resp
		}
	}

	//================================================== GET ==================================================//
	/**
	 * GET request fleksibel:
	 * - Bisa menerima query params (Map<String,String>)
	 * - Header tambahan (Map)
	 * - Cookie opsional
	 * - Validasi status code
	 * - Parsing respons otomatis ke JSON (jika valid), fallback ke String
	 *
	 * @param url endpoint (boleh sudah ada query)
	 * @param query Map query params (akan di-URL-encode)
	 * @param headers header tambahan (mis. Authorization)
	 * @param cookie optional Cookie header
	 * @param expectedStatus status code yang diharapkan (default 200)
	 * @return Object hasil parse JSON atau String response mentah
	 *
	 // GET sederhana tanpa query
	 def resp1 = AppsRequestObject.getRequest(
	 "https://api.example.com/profile",
	 [:],
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}"],
	 "",
	 200
	 )
	 // GET dengan query params
	 def resp2 = AppsRequestObject.getRequest(
	 "https://api.example.com/items",
	 [page: "1", size: "50", search: "monitor 27 inch"],
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}", "Accept": "application/json"],
	 "",
	 200
	 )
	 // GET dengan URL yang sudah punya query (akan di-merge/override)
	 def resp3 = AppsRequestObject.getRequest(
	 "https://api.example.com/items?size=10",
	 [size: "25", sort: "name,asc"], // override size=25
	 ["Authorization": "Bearer ${GlobalVariable.API_TOKEN}"],
	 "",
	 200
	 )
	 */
	@Keyword
	static def getRequest(String url, Map<String, String> query = [:], Map<String, String> headers = [:], String cookie = '', int expectedStatus = 200) {
		// Bangun URL final + query
		String finalUrl = buildUrlWithQuery(url, query)

		KeywordUtil.logInfo("==========[GET DEBUG LOG]==========")
		KeywordUtil.logInfo("Target URL      : ${finalUrl}")
		KeywordUtil.logInfo("Headers (count) : ${headers?.size() ?: 0}")
		KeywordUtil.logInfo("Cookie          : ${cookie ? 'Present' : 'Missing'}")
		KeywordUtil.logInfo("Expected Status : ${expectedStatus}")
		KeywordUtil.logInfo("===================================")

		RequestObject req = new RequestObject("GET Flexible Request")
		req.setRestUrl(finalUrl)
		req.setRestRequestMethod('GET')

		// Setup headers
		List<TestObjectProperty> headerProps = []

		// Default Accept JSON (bisa di-override oleh headers masukan)
		headerProps.add(new TestObjectProperty('Accept', ConditionType.EQUALS, 'application/json'))
		headerProps.add(new TestObjectProperty('User-Agent', ConditionType.EQUALS, 'KatalonGet/1.0'))

		// Header tambahan
		headers?.each { k, v ->
			headerProps.add(new TestObjectProperty(k, ConditionType.EQUALS, v))
		}

		// Cookie optional
		if (cookie) {
			headerProps.add(new TestObjectProperty('Cookie', ConditionType.EQUALS, cookie))
		}

		req.setHttpHeaderProperties(headerProps)

		// Kirim request
		ResponseObject response = WS.sendRequest(req)

		// Validasi status code
		int actualStatus = response.getStatusCode()
		KeywordUtil.logInfo("==========[SERVER RESPONSE]==========")
		KeywordUtil.logInfo("HTTP Code : ${actualStatus}")
		KeywordUtil.logInfo("=====================================")

		if (actualStatus != expectedStatus) {
			KeywordUtil.markFailedAndStop("Status code mismatch. Expected: ${expectedStatus}, Actual: ${actualStatus}")
		}

		// Ambil response body
		String responseText = response.getResponseText()
		if (!responseText) {
			KeywordUtil.markWarning("Response body kosong.")
			return null
		}

		// Coba parse sebagai JSON
		try {
			def json = new JsonSlurper().parseText(responseText)
			return json
		} catch (Exception e) {
			KeywordUtil.logInfo("Response bukan JSON valid. Kembalikan teks biasa.")
			return responseText
		}
	}

	//================================================== HELPER ==================================================//
	/**
	 * Utility untuk menyusun URL + query params.
	 * - Merge query yang sudah ada di URL dengan query Map yang dikirim.
	 * - Lakukan URL-encode terhadap key & value.
	 */
	private static String buildUrlWithQuery(String baseUrl, Map<String, String> query) {
		if (!query || query.isEmpty()) {
			return baseUrl
		}

		// Pisahkan base path dan query existing
		String path = baseUrl
		String existingQuery = ''
		int qIdx = baseUrl.indexOf('?')
		if (qIdx >= 0) {
			path = baseUrl.substring(0, qIdx)
			existingQuery = baseUrl.substring(qIdx + 1)
		}

		// Konversi existing query ke Map sementara
		Map<String, String> merged = [:]
		if (existingQuery) {
			existingQuery.split('&').each { pair ->
				if (pair?.trim()) {
					def kv = pair.split('=', 2)
					String k = kv[0]
					String v = kv.length > 1 ? kv[1] : ''
					merged[k] = v
				}
			}
		}

		// Tambah/override dengan query yang baru
		query.each { k, v ->
			String key = URLEncoder.encode(String.valueOf(k), 'UTF-8')
			String val = URLEncoder.encode(v == null ? '' : String.valueOf(v), 'UTF-8')
			merged[key] = val
		}

		// Susun kembali menjadi string query
		String finalQuery = merged.collect { k, v -> "${k}=${v}" }.join('&')

		return finalQuery ? "${path}?${finalQuery}" : path
	}
}
