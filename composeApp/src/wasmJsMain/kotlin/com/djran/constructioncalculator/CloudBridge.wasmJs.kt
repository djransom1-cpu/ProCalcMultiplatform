package com.djran.constructioncalculator

import kotlinx.coroutines.await
import kotlin.js.Promise

actual val cloudSyncSupported: Boolean = true

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsCloudCall(function: String, arg: String): Promise<JsString> = js("""
    (window.ProCalcCloud && window.ProCalcCloud[function])
        ? window.ProCalcCloud[function](arg)
        : Promise.resolve(JSON.stringify({ error: "Couldn't load cloud sync. Refresh the page and try again." }))
""")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual suspend fun callCloudBridge(function: String, arg: String): String =
    jsCloudCall(function, arg).await<JsString>().toString()
