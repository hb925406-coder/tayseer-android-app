package com.tayseer.app.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtils {

    // الحصول على تاريخ اليوم الهجري بصيغة واضحة (مثال: 1448/05/15)
    fun getTodayHijri(): String {
        return try {
            val hijrahDate = HijrahDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
            formatter.format(hijrahDate)
        } catch (e: Exception) {
            // بديل بسيط في حال عدم دعم بعض الأنظمة القديمة
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date())
        }
    }

    // الحصول على تاريخ اليوم الميلادي (مثال: 2026/10/26)
    fun getTodayGregorian(): String {
        return try {
            val localDate = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH)
            formatter.format(localDate)
        } catch (e: Exception) {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            sdf.format(Date())
        }
    }

    // تسميات حالات الحضور الست باللغة العربية
    fun getStatusLabel(status: String): String {
        return when (status) {
            "PRESENT" -> "حاضر"
            "ABSENT" -> "غائب"
            "EXCUSED" -> "مستأذن"
            "LATE" -> "متأخر"
            "ABSENT_WITH_EXCUSE" -> "غائب بعذر"
            "ESCAPED" -> "هارب"
            else -> "حاضر"
        }
    }

    // تسميات التقييمات والسلوك
    fun getEvaluationLabel(category: String, customName: String = ""): String {
        return when (category) {
            "MEMORIZED" -> "حفظه"
            "NOT_MEMORIZED" -> "لم يحفظ"
            "EXCELLENT" -> "مميز"
            "POLITE" -> "مؤدب"
            "FORGOT_BOOKS" -> "نسيان الكتب"
            "CUSTOM" -> if (customName.isNotBlank()) customName else "مخصص"
            else -> category
        }
    }
}
