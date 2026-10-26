package com.tayseer.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Level
import com.tayseer.app.data.Student
import com.tayseer.app.util.StudentImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).appDao()

    val levels = dao.getAllLevels()
    val unresolvedAlerts = dao.getUnresolvedAlerts()

    private val _selectedLevelId = MutableStateFlow<Long?>(null)
    val selectedLevelId: StateFlow<Long?> = _selectedLevelId.asStateFlow()

    private val _currentDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    fun selectLevel(levelId: Long) {
        _selectedLevelId.value = levelId
    }

    fun getStudentsForCurrentLevel(levelId: Long) = dao.getStudentsByLevel(levelId)

    fun getAttendanceForDate(date: String) = dao.getAttendanceByDate(date)

    fun getStudentEvaluations(studentId: Long) = dao.getEvaluationsByStudent(studentId)

    fun addLevel(name: String, section: String = "") {
        viewModelScope.launch {
            dao.insertLevel(Level(name = name, section = section))
        }
    }

    fun addStudent(levelId: Long, name: String, phone: String = "", parentPhone: String = "") {
        viewModelScope.launch {
            dao.insertStudent(
                Student(
                    levelId = levelId,
                    name = name,
                    phone = phone,
                    parentPhone = parentPhone
                )
            )
        }
    }

    fun importStudentsFromFile(uri: Uri, levelId: Long, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val students = StudentImporter.parseCsvOrTxt(getApplication(), uri, levelId)
            if (students.isNotEmpty()) {
                dao.insertStudents(students)
            }
            withContext(Dispatchers.Main) {
                onComplete(students.size)
            }
        }
    }

    fun recordAttendance(studentId: Long, status: String, excuseReason: String = "", excuseType: String = "") {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                date = _currentDate.value,
                status = status,
                excuseReason = excuseReason,
                excuseType = excuseType,
                isResolved = status in listOf("حاضر", "غائب بعذر")
            )
            dao.insertAttendanceList(listOf(record))
        }
    }

    fun addEvaluation(studentId: Long, type: String, note: String = "") {
        viewModelScope.launch {
            dao.insertEvaluation(
                EvaluationRecord(
                    studentId = studentId,
                    date = _currentDate.value,
                    type = type,
                    note = note
                )
            )
        }
    }

    fun resolveAlert(record: AttendanceRecord, excuseReason: String, excuseType: String) {
        viewModelScope.launch {
            dao.updateAttendance(
                record.copy(
                    excuseReason = excuseReason,
                    excuseType = excuseType,
                    isResolved = true
                )
            )
        }
    }
}
