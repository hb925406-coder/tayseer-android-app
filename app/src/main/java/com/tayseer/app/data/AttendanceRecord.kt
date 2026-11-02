package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var studentId: Long = 0L,
    var date: String = "",
    var status: String = "PRESENT",
    var reasonNote: String? = null,
    var isFollowedUp: Boolean = false,
    var followUpMethod: String? = null,
    var followUpNote: String? = null
)
