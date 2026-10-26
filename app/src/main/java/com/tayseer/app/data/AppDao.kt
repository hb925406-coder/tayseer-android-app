package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- المستويات والحلقات ---
    @Query("SELECT * FROM levels ORDER BY id ASC")
    fun getAllLevels(): Flow<List<Level>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: Level): Long

    @Delete
    suspend fun deleteLevel(level: Level)

    // --- الطلاب ---
    @Query("SELECT * FROM students WHERE levelId = :levelId AND isArchived = 0 ORDER BY name ASC")
    fun getStudentsByLevel(levelId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllActiveStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    // --- الحضور والغياب ---
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE status IN ('غائب', 'متأخر', 'هارب') AND isResolved = 0 ORDER BY date DESC")
    fun getUnresolvedAlerts(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    // --- التقييمات ---
    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    fun getEvaluationsForStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationRecord): Long
}
