package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus(val arabicLabel: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    TRUANT("هارب");

    companion object {
        fun fromName(name: String): AttendanceStatus {
            return try {
                valueOf(name)
            } catch (e: Exception) {
                PRESENT
            }
        }
    }
}

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["studentId", "date"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: String = AttendanceStatus.PRESENT.name,
    val reasonNote: String = ""
) {
    val statusEnum: AttendanceStatus
        get() = AttendanceStatus.fromName(status)
}
