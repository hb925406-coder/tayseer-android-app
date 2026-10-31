package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String // ADMIN, TEACHER, PARENT
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val section: String,
    val parentPhone: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: String, // حاضر, غائب, مستأذن, متأخر, غائب بعذر, هارب
    val excuseType: String = "",
    val excuseReason: String = "",
    val isResolved: Boolean = false
)

@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val evaluationType: String
)

@Entity(tableName = "custom_evaluations")
data class CustomEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String
)
