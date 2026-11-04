package com.tayseer.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "studentId")
    val studentId: Long = 0L,

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
