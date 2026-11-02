package com.tayseer.app.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtils {

    // الحصول على التاريخ الميلادي القياسي الحالي (للتخزين في قاعدة البيانات YYYY-MM-DD)
    fun getTodayGregorian(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return sdf.format(Date())
    }

    // تحويل التاريخ الميلادي (YYYY-MM-DD) إلى تاريخ هجري منسق للعرض
    fun formatToHijri(gregorianDateString: String): String {
        return try {
            val parts = gregorianDateString.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val gDate = LocalDate.of(year, month, day)
            val hDate = HijrahDate.from(gDate)

            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy هـ", Locale("ar"))
            hDate.format(formatter)
        } catch (e: Exception) {
            gregorianDateString
        }
    }

    // الحصول على التاريخ الهجري الحالي للعرض المباشر
    fun getTodayHijriFormatted(): String {
        return formatToHijri(getTodayGregorian())
    }

    // تنسيق التاريخ الميلادي بصيغة عربية أنيقة
    fun formatGregorianArabic(gregorianDateString: String): String {
        return try {
            val parts = gregorianDateString.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val gDate = LocalDate.of(year, month, day)
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy م", Locale("ar"))
            gDate.format(formatter)
        } catch (e: Exception) {
            gregorianDateString
        }
    }
}
