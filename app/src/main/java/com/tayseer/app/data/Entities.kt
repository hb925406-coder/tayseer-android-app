package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// جدول المستويات والحلقات الدراسية
@Entity(tableName = "levels")
data class Level(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val section: String = ""
)

// جدول الطلاب
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val levelId: Long,
    val name: String,
    val phone: String = "",
    val parentPhone: String = "",
    val isArchived: Boolean = false
)

// جدول سجلات الحضور والغياب
@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val status: String, // حاضر، غائب، مستأذن، متأخر، غائب بعذر، هارب
    val excuseReason: String = "",
    val excuseType: String = "", // ورقة، اتصال، واتساب، SMS
    val isResolved: Boolean = false
)

// جدول التقييمات
@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val type: String, // حفظه، لم يحفظ، مميز، مؤدب، نسيان الكتب...
    val notes: String = ""
)
