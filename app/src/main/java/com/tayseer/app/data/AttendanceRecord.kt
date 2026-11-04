package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0L,

    @ColumnInfo(name = "date")
    val date: String = "",

    @ColumnInfo(name = "status")
    val status: String = "حاضر",

    @ColumnInfo(name = "reason")
    val reason: String = "",

    @ColumnInfo(name = "isFollowedUp")
    val isFollowedUp: Boolean = true,

    @ColumnInfo(name = "followUpMethod")
    val followUpMethod: String = "",

    @ColumnInfo(name = "followUpNote")
    val followUpNote: String = ""
)
