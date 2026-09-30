package com.djran.constructioncalculator

actual val cloudSyncSupported: Boolean = false

actual suspend fun callCloudBridge(function: String, arg: String): String =
    """{"error":"Cloud sync is available in the Android app and on the web"}"""
