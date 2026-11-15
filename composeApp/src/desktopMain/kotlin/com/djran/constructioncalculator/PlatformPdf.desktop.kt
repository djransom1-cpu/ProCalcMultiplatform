package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider = DesktopPdfExportProvider()

/**
 * Same printable report as the web build: opens it in the default browser, whose print
 * dialog offers "Save as PDF" (or Microsoft Print to PDF).
 */
class DesktopPdfExportProvider : PdfExportProvider {
    override fun share(projectName: String, sections: List<SummarySection>) {
        try {
            val dir = File(System.getProperty("java.io.tmpdir"), "ProCalc").apply { mkdirs() }
            val f = File(dir, "${projectName.replace(Regex("[^A-Za-z0-9_-]"), "_")}_Summary.html")
            f.writeText(buildHtml(projectName, sections))
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(f.toURI())
        } catch (e: Throwable) {
            println("Desktop PDF error: ${e.message}")
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
                append(java.util.Base64.getEncoder().encodeToString(img.png)).append("\">")
                append("<figcaption>").append(escape(img.caption)).append("</figcaption></figure>")
            }
        }
        append("<script>window.onload=function(){setTimeout(function(){window.print()},400)}</script></body></html>")
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
