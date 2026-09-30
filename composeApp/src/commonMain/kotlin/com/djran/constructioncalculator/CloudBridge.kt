package com.djran.constructioncalculator

/**
 * Whether this build can reach the Android app's cloud projects. Only the web build does: phone
 * users sync from the native Android app, which owns Google Sign-In and the Firebase config.
 */
expect val cloudSyncSupported: Boolean

/**
 * Calls one function of the web cloud bridge (procalc-cloud.js) and returns its raw JSON reply,
 * `{"ok": ...}` or `{"error": "..."}`.
 */
expect suspend fun callCloudBridge(function: String, arg: String): String
