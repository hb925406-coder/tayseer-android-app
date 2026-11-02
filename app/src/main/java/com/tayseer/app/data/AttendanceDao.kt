package com.tayseer.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getRecord(studentId: Long, date: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordBulkAttendance(records: List<AttendanceRecord>): List<Long>

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND status = :status")
    suspend fun countStatusForStudent(studentId: Long, status: String): Int

    // دوال قسم التنبيهات والمتابعة
    @Query("SELECT * FROM attendance WHERE (status = 'ABSENT' OR status = 'LATE' OR status = 'TRUANT') AND followUpResolved = 0 ORDER BY date DESC")
    fun getPendingFollowUps(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance SET followUpResolved = 1, followUpMethod = :method, reasonNote = :note WHERE id = :recordId")
    suspend fun resolveFollowUp(recordId: Long, method: String, note: String?)
}
