package com.tayseer.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val section: String = "",
    val levelId: Long = 1,
    val phone: String = "",
    val parentPhone: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "date"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: String, // حاضر، غائب، مستأذن، متأخر، غائب بعذر، هارب
    val excuseReason: String? = null,
    val excuseType: String? = null, // ورقة من ولي الأمر، اتصال، رسالة واتساب، رسالة SMS
    val isResolved: Boolean = false // هل تمت معالجة وتوثيق العذر أم لا تزال بحاجة متابعة
)

@Entity(
    tableName = "evaluation_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"])
    ]
)
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val evaluationType: String, // حفظه، لم يحفظ، مميز، مؤدب، نسيان الكتب
    val notes: String? = null
)

@Dao
interface AppDao {
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>): List<Long>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    // استعلام لجلب حالات الغياب والتأخر التي تحتاج متابعة
    @Query("SELECT * FROM attendance_records WHERE status IN ('غائب', 'متأخر', 'غائب بعذر') AND isResolved = 0 ORDER BY date DESC")
    fun getPendingAlerts(): Flow<List<AttendanceRecord>>

    // تحديث وتوثيق العذر لإزالته من التنبيهات
    @Query("UPDATE attendance_records SET excuseType = :excuseType, excuseReason = :excuseReason, isResolved = 1 WHERE id = :recordId")
    suspend fun resolveAlert(recordId: Long, excuseType: String, excuseReason: String)

    @Query("SELECT * FROM evaluation_records WHERE studentId = :studentId ORDER BY id DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(record: EvaluationRecord)
}

@Database(
    entities = [Student::class, AttendanceRecord::class, EvaluationRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

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
