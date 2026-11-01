package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Role {
    ADMIN,
    TEACHER,
    PARENT
}

enum class AttendanceStatus(val arabicTitle: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    ESCAPED("هارب")
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "username")
    val username: String = "",

    @ColumnInfo(name = "passwordHash")
    val passwordHash: String = "",

    @ColumnInfo(name = "role")
    val role: Role = Role.ADMIN,

    @ColumnInfo(name = "assignedSection")
    val assignedSection: String = "",

    @ColumnInfo(name = "assignedSubject")
    val assignedSubject: String = "",

    @ColumnInfo(name = "studentId")
    val studentId: Long? = null
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "grade")
    val grade: String = "",

    @ColumnInfo(name = "section")
    val section: String = "",

    @ColumnInfo(name = "groupName")
    val groupName: String = "",

    @ColumnInfo(name = "parentPhone")
    val parentPhone: String = "",

    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true
) {
    val isArchived: Boolean
        get() = !isActive
}

@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0,

    @ColumnInfo(name = "date")
    val date: String = "",

    @ColumnInfo(name = "status")
    val status: String = "حاضر",

    @ColumnInfo(name = "reasonNote")
    val reasonNote: String = "",

    @ColumnInfo(name = "excuseType")
    val excuseType: String = "",

    @ColumnInfo(name = "excuseReason")
    val excuseReason: String = "",

    @ColumnInfo(name = "isResolved")
    val isResolved: Boolean = false
) {
    val statusEnum: AttendanceStatus
        get() = when (status) {
            "حاضر", "PRESENT" -> AttendanceStatus.PRESENT
            "غائب", "ABSENT" -> AttendanceStatus.ABSENT
            "مستأذن", "EXCUSED" -> AttendanceStatus.EXCUSED
            "متأخر", "LATE" -> AttendanceStatus.LATE
            "غائب بعذر", "ABSENT_WITH_EXCUSE" -> AttendanceStatus.ABSENT_WITH_EXCUSE
            "هارب", "ESCAPED" -> AttendanceStatus.ESCAPED
            else -> AttendanceStatus.PRESENT
        }
}

@Entity(tableName = "custom_evaluations")
data class CustomEvaluation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "isPositive")
    val isPositive: Boolean = true
)

@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0,

    @ColumnInfo(name = "date")
    val date: String = "",

    @ColumnInfo(name = "evaluationType")
    val evaluationType: String = "",

    @ColumnInfo(name = "note")
    val note: String = ""
)
