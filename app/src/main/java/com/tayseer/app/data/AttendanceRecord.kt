package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: String,
    val reasonNote: String? = null,
    val isFollowedUp: Boolean = false,
    val followUpMethod: String? = null,
    val followUpNote: String? = null
) {
    val statusEnum: AttendanceStatus
        get() = try {
            AttendanceStatus.valueOf(status)
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
}
