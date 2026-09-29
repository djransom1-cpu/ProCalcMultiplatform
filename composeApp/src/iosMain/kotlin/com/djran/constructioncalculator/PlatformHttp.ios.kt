package com.djran.constructioncalculator

actual suspend fun httpRequest(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?
): HttpResult {
    // iOS cloud sync stub
    return HttpResult(0, "Cloud sync isn't available on iOS yet")
}
