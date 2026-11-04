package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long = 0,
    val date: String = "",
    val status: String = "حاضر",
    val reasonNote: String = "",
    val isFollowedUp: Boolean = false,
    val followUpMethod: String = "",
    val followUpNote: String = ""
)
