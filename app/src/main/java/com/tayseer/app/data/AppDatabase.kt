package com.tayseer.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 1. جدول سجلات الحضور والغياب
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
    val status: String = "حاضر",

    @ColumnInfo(name = "reasonNote")
    val reasonNote: String = "",

    @ColumnInfo(name = "isFollowedUp")
    val isFollowedUp: Boolean = false,

    @ColumnInfo(name = "followUpMethod")
    val followUpMethod: String = "",

    @ColumnInfo(name = "followUpNote")
    val followUpNote: String = ""
)

// 2. جدول سجلات التقييم
@Entity(tableName = "evaluations")
data class EvaluationRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0,

    @ColumnInfo(name = "date")
    val date: String = "",

    @ColumnInfo(name = "evaluationType")
    val evaluationType: String = "",

    @ColumnInfo(name = "note")
    val note: String = ""
)

// 3. جدول التقييمات المخصصة
@Entity(tableName = "custom_evaluations")
data class CustomEvaluation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "isPositive")
    val isPositive: Boolean = true
)

// واجهة التعامل مع الحضور والغياب
@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceListByDateSync(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getRecordForStudent(studentId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getAttendanceForStudentSync(studentId: Long): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE (status = 'غائب' OR status = 'متأخر' OR status = 'هارب' OR status = 'ABSENT' OR status = 'LATE' OR status = 'TRUANT') AND isFollowedUp = 0 ORDER BY date DESC")
    fun getPendingAlerts(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Query("UPDATE attendance_records SET isFollowedUp = 1, followUpMethod = :method, followUpNote = :note WHERE id = :recordId")
    suspend fun resolveFollowUp(recordId: Long, method: String, note: String)
}

// واجهة التعامل مع الطلاب
@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE academicYear = :year AND isArchived = 0 ORDER BY name ASC")
    fun getStudentsByYear(year: String): Flow<List<Student>>

    @Query("SELECT DISTINCT section FROM students WHERE isArchived = 0 ORDER BY section ASC")
    fun getActiveSections(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("UPDATE students SET isArchived = 1 WHERE id = :studentId")
    suspend fun archiveStudent(studentId: Long)

    @Query("UPDATE students SET isArchived = 1 WHERE academicYear = :currentYear")
    suspend fun archiveCurrentYearStudents(currentYear: String)
}

// واجهة التعامل مع التقييمات
@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getEvaluationsForStudentSync(studentId: Long): List<EvaluationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationRecord)

    @Query("SELECT * FROM custom_evaluations ORDER BY id ASC")
    fun getAllCustomEvaluations(): Flow<List<CustomEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomEvaluation(evaluation: CustomEvaluation)

    @Delete
    suspend fun deleteCustomEvaluation(evaluation: CustomEvaluation)
}

// تعريف قاعدة البيانات الكاملة
@Database(
    entities = [
        Student::class,
        AttendanceRecord::class,
        EvaluationRecord::class,
        CustomEvaluation::class
    ],
    version = 3,
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
