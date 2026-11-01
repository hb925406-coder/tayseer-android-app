package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttendanceStatus(val arabicTitle: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    ESCAPED("هارب")
}

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "grade")
    val grade: String = "",

    @ColumnInfo(name = "groupName")
    val groupName: String = "",

    @ColumnInfo(name = "parentPhone")
    val parentPhone: String = "",

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false
)

@Entity(tableName = "attendance_records")
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
    val reasonNote: String = "",

    @ColumnInfo(name = "isResolved")
    val isResolved: Boolean = false
) {
    val statusEnum: AttendanceStatus
        get() = try {
            AttendanceStatus.valueOf(status)
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
}
