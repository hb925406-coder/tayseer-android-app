package com.tayseer.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.CsvImporter
import com.tayseer.app.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayAttendance: StateFlow<List<AttendanceRecord>> = _currentDate.flatMapLatest { date ->
        attendanceDao.getAttendanceByDate(date)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    init {
        loadStudents()
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadStudents() {
        viewModelScope.launch {
            studentDao.getAllStudents().collect { list ->
                _students.value = list
            }
        }
    }

    fun addStudent(student: Student) {
        viewModelScope.launch {
            studentDao.insertStudent(student)
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            studentDao.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            studentDao.deleteStudent(student)
        }
    }

    fun importStudentsFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val parsedStudents = CsvImporter.parseCsv(getApplication(), uri)
                if (parsedStudents.isNotEmpty()) {
                    studentDao.insertStudents(parsedStudents)
                    _importStatusMessage.value = "تم استيراد ${parsedStudents.size} طالب بنجاح"
                } else {
                    _importStatusMessage.value = "الملف فارغ أو تنسيقه غير متوافق"
                }
            } catch (e: Exception) {
                _importStatusMessage.value = "حدث خطأ أثناء استيراد الملف"
            }
        }
    }

    fun setAllPresentForToday() {
        viewModelScope.launch {
            val date = _currentDate.value
            val currentList = _students.value
            if (currentList.isNotEmpty()) {
                val records = currentList.map { student ->
                    AttendanceRecord(
                        studentId = student.id,
                        date = date,
                        status = AttendanceStatus.PRESENT
                    )
                }
                attendanceDao.recordBulkAttendance(records)
                _importStatusMessage.value = "تم تحضير جميع الطلاب كـ (حاضر)"
            }
        }
    }

    fun updateStudentAttendance(studentId: Long, status: AttendanceStatus, note: String = "") {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                date = _currentDate.value,
                status = status,
                reasonNote = note
            )
            attendanceDao.recordAttendance(record)
        }
    }

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }
}
