package com.tayseer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDao
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainViewModel(private val dao: AppDao) : ViewModel() {

    private val gregorianFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val todayDateString: String = gregorianFormat.format(Date())

    val todayHijriString: String = try {
        val hijrahDate = HijrahDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
        formatter.format(hijrahDate) + " هـ"
    } catch (e: Exception) {
        "1448 هـ"
    }

    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _displayDate = MutableStateFlow(todayHijriString)
    val displayDate: StateFlow<String> = _displayDate.asStateFlow()

    private val _isHijri = MutableStateFlow(true)
    val isHijri: StateFlow<Boolean> = _isHijri.asStateFlow()

    private val _selectedSection = MutableStateFlow("الكل")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    // نظام تسجيل الدخول والمستخدم الحالي
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val students: StateFlow<List<Student>> = dao.getAllActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = dao.getPendingAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAttendanceForDate(todayDateString)
        initializeDefaultUsers()
    }

    // إنشاء حساب مدير تجريبي تلقائياً إذا كانت قاعدة البيانات جديدة
    private fun initializeDefaultUsers() {
        viewModelScope.launch {
            if (dao.getUsersCount() == 0) {
                dao.insertUser(User(username = "admin", passwordHash = "123456", role = "ADMIN"))
                dao.insertUser(User(username = "teacher", passwordHash = "123456", role = "TEACHER"))
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val user = dao.login(username.trim(), password.trim())
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
            } else {
                _loginError.value = "اسم المستخدم أو كلمة المرور غير صحيحة"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
    }

    fun toggleCalendarType() {
        _isHijri.value = !_isHijri.value
        _displayDate.value = if (_isHijri.value) todayHijriString else todayDateString
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
                excuseType = excuseType,
                isResolved = (status == "حاضر" || status == "مستأذن")
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
                    status = "حاضر",
                    isResolved = true
                )
                dao.insertAttendance(record)
            }
        }
    }

    fun resolveAlert(recordId: Long, excuseType: String, excuseReason: String) {
        viewModelScope.launch {
            dao.resolveAlert(recordId, excuseType, excuseReason)
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
