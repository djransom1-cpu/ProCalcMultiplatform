package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider = WasmPdfExportProvider()

/**
 * Opens the report as a printable page; the browser's print dialog offers "Save as PDF",
 * matching the PDF the Android app shares.
 */
class WasmPdfExportProvider : PdfExportProvider {
    override fun share(projectName: String, sections: List<SummarySection>) {
        try {
            openPrintWindow(buildHtml(projectName, sections))
        } catch (e: Throwable) {
            println("Wasm PDF Error: ${e.message}")
        }
    }

    private fun buildHtml(projectName: String, sections: List<SummarySection>): String = buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>")
        append(escape("${projectName.replace(" ", "_")}_Summary"))
        append("</title><style>")
        append("body{font-family:Arial,Helvetica,sans-serif;margin:32px;color:#000}")
        append("h1{font-size:20px;margin:0 0 24px}h2{font-size:15px;margin:24px 0 6px}")
        append("table{width:100%;border-collapse:collapse;font-size:12px;page-break-inside:auto}")
        append("th{text-align:left;border-bottom:1px solid #000;padding:4px 6px}")
        append("td{padding:3px 6px}tr{page-break-inside:avoid}")
        append("</style></head><body>")
        append("<h1>Project Summary: ").append(escape(projectName)).append("</h1>")
        sections.forEach { section ->
            append("<h2>").append(escape(section.title)).append("</h2>")
            if (section.headers.isNotEmpty() || section.rows.isNotEmpty()) {
                append("<table>")
                if (section.headers.isNotEmpty()) {
                    append("<tr>")
                    section.headers.forEach { append("<th>").append(escape(it)).append("</th>") }
                    append("</tr>")
                }
                section.rows.forEach { row ->
                    append("<tr>")
                    row.forEach { append("<td>").append(escape(it)).append("</td>") }
                    append("</tr>")
                }
                append("</table>")
            }
        }
        append("</body></html>")
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun openPrintWindow(html: String) {
    js("""
        var w = window.open('', '_blank');
        if (w) {
            w.document.open();
            w.document.write(html);
            w.document.close();
            w.focus();
            setTimeout(function () { w.print(); }, 300);
        }
    """)
}
