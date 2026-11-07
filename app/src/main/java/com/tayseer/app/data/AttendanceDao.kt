package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceListByDateSync(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getRecordForStudent(studentId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    suspend fun getAttendanceForStudentSync(studentId: Long): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE statusEnum IN ('ABSENT', 'LATE', 'ESCAPED') AND isFollowedUp = 0")
    fun getPendingAlerts(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance_records SET isFollowedUp = 1, followUpMethod = :method, followUpNote = :note WHERE id = :recordId")
    suspend fun resolveFollowUp(recordId: Long, method: String, note: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)
}
