package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

interface PdfExportProvider {
    fun share(projectName: String, sections: List<SummarySection>)
}

@Composable
expect fun rememberPdfExportProvider(): PdfExportProvider
