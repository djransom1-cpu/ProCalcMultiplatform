package com.djran.constructioncalculator

class HttpResult(val status: Int, val body: String)

/**
 * Minimal HTTP client used by cloud sync. Only GET and POST are ever issued so every platform
 * can use its simplest built-in client. A status of 0 means the request never reached the server
 * (offline, DNS, CORS...) and [HttpResult.body] then holds the error message.
 */
expect suspend fun httpRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String? = null
): HttpResult
