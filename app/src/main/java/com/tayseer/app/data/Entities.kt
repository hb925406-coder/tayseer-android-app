package com.tayseer.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// جدول المستخدمين للصلاحيات المحلية
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String, // ADMIN (إدارة), TEACHER (معلم), PARENT (ولي أمر)
    val linkedId: Long? = null // معرف الطالب المرتبط في حال كان الحساب لولي أمر
)

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
    val isResolved: Boolean = false
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
    // إدارة المستخدمين
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    // إدارة الطلاب
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): Student?

    @Query("SELECT * FROM students WHERE section = :section AND isActive = 1 ORDER BY name ASC")
    fun getStudentsBySection(section: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>): List<Long>

    // الحضور والغياب
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    // التنبيهات والأعذار
    @Query("SELECT * FROM attendance_records WHERE status IN ('غائب', 'متأخر', 'غائب بعذر') AND isResolved = 0 ORDER BY date DESC")
    fun getPendingAlerts(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance_records SET excuseType = :excuseType, excuseReason = :excuseReason, isResolved = 1 WHERE id = :recordId")
    suspend fun resolveAlert(recordId: Long, excuseType: String, excuseReason: String)

    // التقييمات
    @Query("SELECT * FROM evaluation_records WHERE studentId = :studentId ORDER BY id DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(record: EvaluationRecord)
}

@Database(
    entities = [User::class, Student::class, AttendanceRecord::class, EvaluationRecord::class],
    version = 3,
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
