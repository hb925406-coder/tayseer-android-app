package com.tayseer.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.Student
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    fun generateDailyReportPdf(
        context: Context,
        students: List<Student>,
        records: List<AttendanceRecord>,
        dateString: String,
        section: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            textAlign = Paint.Align.RIGHT
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // ترويسة التقرير
        canvas.drawText("تطبيق تيسير - كشف الحضور والمتابعة اليومي", 297f, 50f, titlePaint)
        canvas.drawText("التاريخ: $dateString", 540f, 90f, headerPaint)
        canvas.drawText("الحلقة / القسم: $section", 540f, 110f, headerPaint)
        canvas.drawText("إجمالي الطلاب: ${students.size}", 540f, 130f, headerPaint)

        canvas.drawLine(40f, 150f, 555f, 150f, linePaint)

        // رؤوس الأعمدة
        canvas.drawText("م", 540f, 175f, headerPaint)
        canvas.drawText("اسم الطالب", 480f, 175f, headerPaint)
        canvas.drawText("الحالة", 200f, 175f, headerPaint)
        canvas.drawText("الحلقة", 100f, 175f, headerPaint)

        canvas.drawLine(40f, 185f, 555f, 185f, linePaint)

        var yPosition = 210f
        students.forEachIndexed { index, student ->
            if (yPosition < 800f) {
                val record = records.find { it.studentId == student.id }
                val status = record?.status ?: "لم يُسجل"

                canvas.drawText("${index + 1}", 540f, yPosition, bodyPaint)
                canvas.drawText(student.name, 480f, yPosition, bodyPaint)
                canvas.drawText(status, 200f, yPosition, bodyPaint)
                canvas.drawText(student.section, 100f, yPosition, bodyPaint)

                canvas.drawLine(40f, yPosition + 10f, 555f, yPosition + 10f, linePaint)
                yPosition += 30f
            }
        }

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "تقرير_تيسير_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
