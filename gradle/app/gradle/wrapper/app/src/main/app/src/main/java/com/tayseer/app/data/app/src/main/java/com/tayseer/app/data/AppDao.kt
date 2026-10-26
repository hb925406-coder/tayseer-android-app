package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- إدارة المستخدمين ---
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    // --- إدارة الحلقات والمجموعات ---
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<StudentGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudentGroup): Long

    // --- إدارة الطلاب ---
    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE groupId = :groupId AND isArchived = 0 ORDER BY name ASC")
    fun getStudentsByGroup(groupId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    // --- سجلات الحضور والغياب ---
    @Query("SELECT * FROM attendance WHERE dateHijri = :dateHijri")
    fun getAttendanceByDate(dateHijri: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY id DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(attendance: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(records: List<AttendanceRecord>)

    // الحالات التي تحتاج متابعة (تنبيهات الغياب والتأخير)
    @Query("SELECT * FROM attendance WHERE followUpStatus = 'PENDING' ORDER BY id DESC")
    fun getPendingFollowUps(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance SET followUpStatus = :status, excuseType = :excuseType, note = :note WHERE id = :recordId")
    suspend fun resolveFollowUp(recordId: Long, status: String, excuseType: String, note: String)

    // --- سجلات التقييم ---
    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY id DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationRecord): Long
}
