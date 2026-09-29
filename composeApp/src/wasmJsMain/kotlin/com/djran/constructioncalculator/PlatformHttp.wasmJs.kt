package com.djran.constructioncalculator

import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

// Resolves to "<status>\n<body>" so only a single string has to cross the JS boundary.
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsFetch(method: String, url: String, headersJson: String, body: String?): Promise<JsString> = js("""
    fetch(url, { method: method, headers: JSON.parse(headersJson), body: body == null ? undefined : body })
        .then(function (r) { return r.text().then(function (t) { return r.status + "\n" + t; }); })
""")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual suspend fun httpRequest(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?
): HttpResult {
    return try {
        val raw = jsFetch(method, url, Json.encodeToString(headers), body).await<JsString>().toString()
        val newline = raw.indexOf('\n')
        HttpResult(raw.substring(0, newline).toInt(), raw.substring(newline + 1))
    } catch (e: Throwable) {
        HttpResult(0, e.message ?: "Network error")
    }
}
