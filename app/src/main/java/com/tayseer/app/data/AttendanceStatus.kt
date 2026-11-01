package com.tayseer.app.data

enum class AttendanceStatus(val arabicLabel: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    EXCUSED("مستأذن"),
    LATE("متأخر"),
    ABSENT_WITH_EXCUSE("غائب بعذر"),
    TRUANT("هارب");

    companion object {
        fun fromName(name: String?): AttendanceStatus {
            return try {
                if (name != null) valueOf(name) else PRESENT
            } catch (e: Exception) {
                PRESENT
            }
        }
    }
}
