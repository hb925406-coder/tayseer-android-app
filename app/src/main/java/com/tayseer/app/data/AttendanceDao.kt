package com.tayseer.app.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @RawQuery(observedEntities = [AttendanceRecord::class])
    fun getAttendanceRaw(query: SupportSQLiteQuery): Flow<List<AttendanceRecord>>

    @RawQuery
    suspend fun getAttendanceRawSync(query: SupportSQLiteQuery): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)
}
