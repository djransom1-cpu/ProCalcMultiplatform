package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider = IosPdfExportProvider()

class IosPdfExportProvider : PdfExportProvider {
    override fun share(projectName: String, sections: List<SummarySection>) {
        // iOS PDF export logic would go here using UIGraphicsPDFRenderer
    }
}
