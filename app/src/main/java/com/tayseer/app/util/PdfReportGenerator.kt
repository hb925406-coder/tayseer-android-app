package com.tayseer.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    // تقرير كشف الحضور اليومي
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

        canvas.drawText("تطبيق تيسير - كشف الحضور والمتابعة اليومي", 297f, 50f, titlePaint)
        canvas.drawText("التاريخ: $dateString", 540f, 90f, headerPaint)
        canvas.drawText("الحلقة / القسم: $section", 540f, 110f, headerPaint)
        canvas.drawText("إجمالي الطلاب: ${students.size}", 540f, 130f, headerPaint)

        canvas.drawLine(40f, 150f, 555f, 150f, linePaint)

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

        val outputFile = File(context.cacheDir, "تقرير_تيسير_يومي_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    // تقرير خلاصة طالب مفصلة
    fun generateStudentSummaryPdf(
        context: Context,
        student: Student,
        attendanceList: List<AttendanceRecord>,
        evaluationList: List<EvaluationRecord>
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

        // الترويسة
        canvas.drawText("تطبيق تيسير - تقرير خلاصة الطالب", 297f, 50f, titlePaint)
        canvas.drawText("اسم الطالب: ${student.name}", 540f, 90f, headerPaint)
        canvas.drawText("الحلقة / الصف: ${student.section}", 540f, 115f, headerPaint)
        canvas.drawText("هاتف ولي الأمر: ${student.parentPhone}", 540f, 140f, headerPaint)

        // إحصائيات الحضور
        val presentCount = attendanceList.count { it.status == "حاضر" }
        val absentCount = attendanceList.count { it.status == "غائب" || it.status == "غائب بعذر" }
        val lateCount = attendanceList.count { it.status == "متأخر" }
        canvas.drawText("إحصائية الحضور: حاضر ($presentCount) | غياب ($absentCount) | تأخر ($lateCount)", 540f, 170f, headerPaint)

        canvas.drawLine(40f, 190f, 555f, 190f, linePaint)

        // قسم سجلات التقييم الأحدث
        canvas.drawText("أحدث التقييمات والسلوك:", 540f, 215f, headerPaint)
        var yPos = 240f
        evaluationList.take(6).forEach { eval ->
            canvas.drawText("• التاريخ: ${eval.date}  -  التقييم: ${eval.evaluationType}", 520f, yPos, bodyPaint)
            yPos += 24f
        }

        yPos += 10f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 25f

        // قسم سجلات الحضور الأخيرة
        canvas.drawText("سجل الحضور الأخير:", 540f, yPos, headerPaint)
        yPos += 25f
        attendanceList.take(8).forEach { att ->
            canvas.drawText("• التاريخ: ${att.date}  -  الحالة: ${att.status}", 520f, yPos, bodyPaint)
            yPos += 24f
        }

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "خلاصة_${student.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
