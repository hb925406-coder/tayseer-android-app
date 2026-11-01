package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Role {
    ADMIN,
    TEACHER,
    PARENT
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "username")
    val username: String = "",

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

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0,

    @ColumnInfo(name = "date")
    val date: String = "",

    @ColumnInfo(name = "status")
    val status: String = "PRESENT",

    @ColumnInfo(name = "reasonNote")
    val reasonNote: String = "",

    @ColumnInfo(name = "isResolved")
    val isResolved: Boolean = false
) {
    val statusEnum: AttendanceStatus
        get() = try {
            AttendanceStatus.valueOf(status)
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
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

@Entity(tableName = "evaluation_records")
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
