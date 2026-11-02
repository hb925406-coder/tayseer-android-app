package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
