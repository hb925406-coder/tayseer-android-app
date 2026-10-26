package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// جدول المستخدمين والأدوار (إدارة، معلم، ولي أمر)
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String, // ADMIN, TEACHER, PARENT
    val linkedStudentId: Long? = null
)

// جدول الأقسام / الحلقات الدراسية
@Entity(tableName = "groups")
data class StudentGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacherId: Long? = null
)

// جدول الطلاب
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long,
    val parentPhone: String = "",
    val nationalId: String = "",
    val isArchived: Boolean = false
)

// جدول الحضور والغياب (يدعم الحالات الست)
@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val dateHijri: String,
    val dateGregorian: String,
    val status: String, // PRESENT, ABSENT, EXCUSED, LATE, ABSENT_WITH_EXCUSE, ESCAPED
    val note: String = "",
    val followUpStatus: String = "NONE", // NONE, PENDING, RESOLVED
    val excuseType: String = "" // PAPER, CALL, WHATSAPP, SMS
)

// جدول التقييمات والسلوك وحفظ القرآن
@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val dateHijri: String,
    val category: String, // MEMORIZED, NOT_MEMORIZED, EXCELLENT, POLITE, FORGOT_BOOKS, CUSTOM
    val customCategoryName: String = "",
    val notes: String = ""
)
