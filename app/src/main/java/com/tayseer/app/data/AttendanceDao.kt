package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(Converters::class)
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getRecord(studentId: Long, date: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordBulkAttendance(records: List<AttendanceRecord>): List<Long>

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Long)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = :status")
    suspend fun countStatusForStudent(studentId: Long, status: AttendanceStatus): Int
}
