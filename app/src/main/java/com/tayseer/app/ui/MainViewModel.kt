package com.tayseer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDao
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.data.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(private val appDao: AppDao) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultUsersExist()
        }
    }

    private suspend fun ensureDefaultUsersExist() {
        try {
            if (appDao.getUsersCount() == 0) {
                appDao.insertUser(User(username = "admin", passwordHash = "123456", role = "ADMIN"))
                appDao.insertUser(User(username = "teacher", passwordHash = "123456", role = "TEACHER"))
                appDao.insertUser(User(username = "parent", passwordHash = "123456", role = "PARENT"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val students: StateFlow<List<Student>> = appDao.getAllActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = appDao.getPendingAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isHijri = MutableStateFlow(true)
    val isHijri: StateFlow<Boolean> = _isHijri.asStateFlow()

    private val _selectedSection = MutableStateFlow("الكل")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    val todayDateString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val displayDate: StateFlow<String> = _isHijri.map { hijri ->
        if (hijri) {
            "1448/05/20 هـ"
        } else {
            todayDateString
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), todayDateString)

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = appDao.getAttendanceByDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _parentStudent = MutableStateFlow<Student?>(null)
    val parentStudent: StateFlow<Student?> = _parentStudent.asStateFlow()

    val parentAttendance: StateFlow<List<AttendanceRecord>> = _parentStudent.flatMapLatest { student ->
        if (student != null) appDao.getAttendanceForStudent(student.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentEvaluations: StateFlow<List<EvaluationRecord>> = _parentStudent.flatMapLatest { student ->
        if (student != null) appDao.getEvaluationsForStudent(student.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            val uName = username.trim()
            val pWord = pass.trim()

            ensureDefaultUsersExist()

            val user = appDao.login(uName, pWord)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                if (user.role == "PARENT") {
                    val activeStudents = appDao.getAllActiveStudents().firstOrNull() ?: emptyList()
                    _parentStudent.value = activeStudents.firstOrNull()
                }
            } else {
                _loginError.value = "بيانات الدخول غير صحيحة"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _parentStudent.value = null
    }

    fun toggleCalendarType() {
        _isHijri.value = !_isHijri.value
    }

    fun setSection(section: String) {
        _selectedSection.value = section
    }

    fun insertStudent(student: Student) {
        viewModelScope.launch {
            appDao.insertStudent(student)
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            appDao.insertStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            appDao.deleteStudentById(student.id)
        }
    }

    fun insertStudentsList(studentsList: List<Student>) {
        viewModelScope.launch {
            appDao.insertStudents(studentsList)
        }
    }

    fun markAttendance(studentId: Long, status: String) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                date = todayDateString,
                status = status
            )
            appDao.insertAttendance(record)
        }
    }

    fun markAllPresent(currentStudents: List<Student>) {
        viewModelScope.launch {
            currentStudents.forEach { student ->
                val record = AttendanceRecord(
                    studentId = student.id,
                    date = todayDateString,
                    status = "حاضر"
                )
                appDao.insertAttendance(record)
            }
        }
    }

    fun markEvaluation(studentId: Long, evaluationType: String) {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = todayDateString,
                evaluationType = evaluationType
            )
            appDao.insertEvaluation(record)
        }
    }

    fun resolveAlert(recordId: Long, excuseType: String, reason: String) {
        viewModelScope.launch {
            appDao.resolveAlert(recordId, excuseType, reason)
        }
    }
}
