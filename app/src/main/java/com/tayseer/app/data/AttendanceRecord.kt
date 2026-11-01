package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

enum class AttendanceStatus(val arabicLabel: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    TRUANT("هارب")
}

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["studentId", "date"], unique = true)
    ]
)
@TypeConverters(Converters::class)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val reasonNote: String = ""
)
