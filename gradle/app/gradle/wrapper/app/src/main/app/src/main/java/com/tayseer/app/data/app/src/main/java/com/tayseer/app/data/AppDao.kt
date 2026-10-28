package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- إدارة الطلاب ---
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    // --- سجلات الحضور ---
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getAttendanceListForStudent(studentId: Long): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Query("SELECT * FROM attendance WHERE isResolved = 0 AND status IN ('غائب', 'متأخر', 'غائب بعذر', 'هارب') ORDER BY date DESC")
    fun getPendingAlerts(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance SET isResolved = 1, excuseType = :excuseType, excuseReason = :excuseReason WHERE id = :recordId")
    suspend fun resolveAlert(recordId: Long, excuseType: String, excuseReason: String)

    // --- سجلات التقييم ---
    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getEvaluationListForStudent(studentId: Long): List<EvaluationRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(record: EvaluationRecord): Long

    // --- نظام المستخدمين وتسجيل الدخول ---
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
}
