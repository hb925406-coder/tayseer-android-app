package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "studentId")
    val studentId: Long,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "reasonNote")
    val reasonNote: String? = null,

    @ColumnInfo(name = "isFollowedUp")
    val isFollowedUp: Boolean = false,

    @ColumnInfo(name = "followUpMethod")
    val followUpMethod: String? = null,

    @ColumnInfo(name = "followUpNote")
    val followUpNote: String? = null
)
