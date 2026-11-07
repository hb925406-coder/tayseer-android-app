package com.tayseer.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val evaluationDao = db.evaluationDao()
    private val customEvaluationDao = db.customEvaluationDao()

    val allStudents: StateFlow<List<Student>> = studentDao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = attendanceDao.getPendingAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceByDate(date)
    }

    fun addStudent(name: String, grade: String, section: String, academicYear: String, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val student = Student(
                name = name,
                grade = grade,
                section = section,
                academicYear = academicYear,
                parentPhone = phone
            )
            studentDao.insertStudent(student)
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            studentDao.updateStudent(student)
        }
    }

    fun recordAttendance(studentId: Long, date: String, status: AttendanceStatus, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = attendanceDao.getRecordForStudent(studentId, date)
            val isFollowUpNeeded = (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LATE || status == AttendanceStatus.ESCAPED)
            val record = existing?.copy(
                statusEnum = status,
                reasonNote = note,
                isFollowedUp = !isFollowUpNeeded
            ) ?: AttendanceRecord(
                studentId = studentId,
                date = date,
                statusEnum = status,
                reasonNote = note,
                isFollowedUp = !isFollowUpNeeded
            )
            attendanceDao.insertAttendance(record)
        }
    }

    fun resolveFollowUp(recordId: Long, method: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceDao.resolveFollowUp(recordId, method, note)
        }
    }

    fun importStudentsFromExcel(context: Context, uri: Uri, grade: String, section: String, academicYear: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)
                    val newStudents = mutableListOf<Student>()
                    for (row in sheet) {
                        if (row.rowNum == 0) continue
                        val nameCell = row.getCell(0) ?: continue
                        val name = nameCell.stringCellValue.trim()
                        if (name.isNotEmpty()) {
                            val phoneCell = row.getCell(1)
                            val phone = phoneCell?.toString()?.trim() ?: ""
                            newStudents.add(
                                Student(
                                    name = name,
                                    grade = grade,
                                    section = section,
                                    academicYear = academicYear,
                                    parentPhone = phone
                                )
                            )
                        }
                    }
                    studentDao.insertStudents(newStudents)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
