package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["studentId", "date"], unique = true)
    ]
)
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
    val reasonNote: String = ""
)
