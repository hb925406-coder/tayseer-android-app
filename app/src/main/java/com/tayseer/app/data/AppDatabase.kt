package com.tayseer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val section: String = "عام",
    val academicYear: String = "1447-1448", // السنة الدراسية
    val isArchived: Boolean = false // أرشفة السنة السابقة
)

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

@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val evaluationType: String,
    val note: String? = null
)

@Entity(tableName = "custom_evaluations")
data class CustomEvaluation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val isPositive: Boolean = true
)

@Database(
    entities = [
        Student::class,
        AttendanceRecord::class,
        EvaluationRecord::class,
        CustomEvaluation::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun evaluationDao(): EvaluationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tayseer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
