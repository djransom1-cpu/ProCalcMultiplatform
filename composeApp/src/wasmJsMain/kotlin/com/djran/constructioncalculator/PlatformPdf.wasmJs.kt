package com.djran.constructioncalculator

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider = WasmPdfExportProvider()

/**
 * Opens the report as a printable page; the browser's print dialog offers "Save as PDF",
 * matching the PDF the Android app shares.
 */
@OptIn(ExperimentalEncodingApi::class)
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
        append("figure{margin:10px 0 14px;page-break-inside:avoid}figure img{width:100%;max-width:640px;border:1px solid #ccc}")
        append("figcaption{font-size:11px;color:#555;margin-top:2px}")
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
            section.images.forEach { img ->
                append("<figure>")
                append("<img alt=\"").append(escape(img.caption)).append("\" src=\"data:image/png;base64,")
                append(Base64.encode(img.png)).append("\">")
                append("<figcaption>").append(escape(img.caption)).append("</figcaption></figure>")
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
