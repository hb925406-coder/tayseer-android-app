package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Levels
    @Query("SELECT * FROM levels ORDER BY name ASC")
    fun getAllLevels(): Flow<List<Level>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: Level): Long

    // Students
    @Query("SELECT * FROM students WHERE levelId = :levelId AND isArchived = 0 ORDER BY name ASC")
    fun getStudentsByLevel(levelId: Long): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    // Attendance
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE isResolved = 0 AND status IN ('غائب', 'متأخر', 'هارب')")
    fun getUnresolvedAlerts(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    // Evaluations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationRecord): Long

    @Query("SELECT * FROM evaluations WHERE studentId = :studentId ORDER BY date DESC")
    fun getEvaluationsByStudent(studentId: Long): Flow<List<EvaluationRecord>>

    @Delete
    suspend fun deleteEvaluation(evaluation: EvaluationRecord)
}
