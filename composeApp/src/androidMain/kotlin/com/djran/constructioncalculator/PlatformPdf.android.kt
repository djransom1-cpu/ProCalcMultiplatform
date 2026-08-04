package com.djran.constructioncalculator

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberPdfExportProvider(): PdfExportProvider {
    val context = LocalContext.current
    return AndroidPdfExportProvider(context)
}

class AndroidPdfExportProvider(private val context: Context) : PdfExportProvider {
    override fun share(projectName: String, sections: List<SummarySection>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        canvas.drawText("Project Summary: $projectName", 40f, y, titlePaint)
        y += 40f

        sections.forEach { section ->
            // Check if we need a new page before section title
            if (y > 750f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText(section.title, 40f, y, headerPaint)
            y += 20f

            // Headers
            var x = 40f
            val columnWidth = (515f / section.headers.size.coerceAtLeast(1))
            section.headers.forEach { header ->
                canvas.drawText(header, x, y, textPaint)
                x += columnWidth
            }
            y += 5f
            canvas.drawLine(40f, y, 555f, y, paint)
            y += 15f

            // Rows
            section.rows.forEach { row ->
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                }

                x = 40f
                row.forEach { cell ->
                    canvas.drawText(cell, x, y, textPaint)
                    x += columnWidth
                }
                y += 15f
            }
            y += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "${projectName.replace(" ", "_")}_Summary.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        // Share the file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Job Summary PDF"))
    }
}
