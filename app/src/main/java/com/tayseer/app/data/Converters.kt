package com.tayseer.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? {
        return value?.let {
            try {
                AttendanceStatus.valueOf(it)
            } catch (e: Exception) {
                AttendanceStatus.PRESENT
            }
        }
    }
}
