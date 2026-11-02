package com.tayseer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val section: String = "عام",
    val academicYear: String = "1447-1448",
    val isArchived: Boolean = false
)
