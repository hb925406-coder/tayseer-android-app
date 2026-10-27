package com.tayseer.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDao
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val dao: AppDao) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormat.format(Date())

    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedSection = MutableStateFlow("الكل")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    val students: StateFlow<List<Student>> = dao.getAllActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    init {
        loadAttendanceForDate(todayDateString)
    }

    fun setDate(date: String) {
        _selectedDate.value = date
        loadAttendanceForDate(date)
    }

    fun setSection(section: String) {
        _selectedSection.value = section
    }

    private fun loadAttendanceForDate(date: String) {
        viewModelScope.launch {
            dao.getAttendanceByDate(date).collect { list ->
                _attendanceRecords.value = list
            }
        }
    }

    fun markAttendance(studentId: Long, status: String, excuseReason: String? = null, excuseType: String? = null) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                date = _selectedDate.value,
                status = status,
                excuseReason = excuseReason,
                excuseType = excuseType
            )
            dao.insertAttendance(record)
        }
    }

    fun markAllPresent(studentList: List<Student>) {
        viewModelScope.launch {
            studentList.forEach { student ->
                val record = AttendanceRecord(
                    studentId = student.id,
                    date = _selectedDate.value,
                    status = "حاضر"
                )
                dao.insertAttendance(record)
            }
        }
    }

    fun markEvaluation(studentId: Long, evaluationType: String, notes: String? = null) {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = _selectedDate.value,
                evaluationType = evaluationType,
                notes = notes
            )
            dao.insertEvaluation(record)
        }
    }

    fun insertStudent(student: Student) {
        viewModelScope.launch {
            dao.insertStudent(student)
        }
    }

    fun insertStudentsList(studentsList: List<Student>) {
        viewModelScope.launch {
            dao.insertStudents(studentsList)
        }
    }
}
