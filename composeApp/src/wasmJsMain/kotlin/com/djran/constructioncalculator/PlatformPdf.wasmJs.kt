package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider = WasmPdfExportProvider()

class WasmPdfExportProvider : PdfExportProvider {
    override fun share(projectName: String, sections: List<SummarySection>) {
        // Web PDF export logic (e.g. print window or js library)
    }
}
