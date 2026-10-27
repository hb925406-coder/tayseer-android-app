package com.tayseer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDao
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val dao: AppDao) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _evaluationRecords = MutableStateFlow<List<EvaluationRecord>>(emptyList())
    val evaluationRecords: StateFlow<List<EvaluationRecord>> = _evaluationRecords.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        loadStudents()
        loadAttendanceForDate(_selectedDate.value)
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        loadAttendanceForDate(date)
    }

    private fun loadStudents() {
        viewModelScope.launch {
            dao.getAllActiveStudents()
                .catch { _students.value = emptyList() }
                .collect { list ->
                    _students.value = list
                }
        }
    }

    fun loadAttendanceForDate(date: String) {
        viewModelScope.launch {
            dao.getAttendanceByDate(date)
                .catch { _attendanceRecords.value = emptyList() }
                .collect { records ->
                    _attendanceRecords.value = records
                }
        }
    }

    fun loadEvaluationsForStudent(studentId: Long) {
        viewModelScope.launch {
            dao.getEvaluationsForStudent(studentId)
                .catch { _evaluationRecords.value = emptyList() }
                .collect { records ->
                    _evaluationRecords.value = records
                }
        }
    }

    fun insertStudent(student: Student) {
        viewModelScope.launch {
            dao.insertStudent(student)
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

    fun markEvaluation(studentId: Long, type: String, noteText: String? = null) {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = _selectedDate.value,
                evaluationType = type,
                notes = noteText
            )
            dao.insertEvaluation(record)
        }
    }
}
