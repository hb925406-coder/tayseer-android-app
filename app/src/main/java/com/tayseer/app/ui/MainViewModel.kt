package com.tayseer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(private val dao: AppDao) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isHijri = MutableStateFlow(true)
    val isHijri: StateFlow<Boolean> = _isHijri.asStateFlow()

    val activeStudents: StateFlow<List<Student>> = dao.getAllActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEvaluations: StateFlow<List<CustomEvaluation>> = dao.getAllCustomEvaluations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = dao.getPendingAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAttendance: StateFlow<List<AttendanceRecord>> = _selectedDate
        .flatMapLatest { date -> dao.getAttendanceByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.getUsersCount() == 0) {
                dao.insertUser(User(username = "admin", passwordHash = "admin123", role = "ADMIN"))
                dao.insertUser(User(username = "teacher", passwordHash = "teacher123", role = "TEACHER"))
            }
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun toggleCalendarType() {
        _isHijri.value = !_isHijri.value
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = dao.login(username, password)
            _currentUser.value = user
            onResult(user != null)
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun addStudent(name: String, section: String, parentPhone: String) {
        viewModelScope.launch {
            dao.insertStudent(Student(name = name, section = section, parentPhone = parentPhone))
        }
    }

    fun setAttendance(studentId: Long, status: String) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                studentId = studentId,
                date = _selectedDate.value,
                status = status,
                isResolved = (status == "حاضر")
            )
            dao.insertAttendance(record)
        }
    }

    // الدالة المسؤولة عن تحضير كافة الطلاب النشطين دفعة واحدة كـ "حاضر"
    fun markAllPresent() {
        viewModelScope.launch {
            val students = activeStudents.value
            val date = _selectedDate.value
            students.forEach { student ->
                val record = AttendanceRecord(
                    studentId = student.id,
                    date = date,
                    status = "حاضر",
                    isResolved = true
                )
                dao.insertAttendance(record)
            }
        }
    }

    fun addEvaluation(studentId: Long, evaluationType: String) {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = _selectedDate.value,
                evaluationType = evaluationType
            )
            dao.insertEvaluation(record)
        }
    }

    fun addCustomEvaluation(title: String) {
        viewModelScope.launch {
            dao.insertCustomEvaluation(CustomEvaluation(title = title))
        }
    }

    fun resolveAlert(recordId: Long, excuseType: String, excuseReason: String) {
        viewModelScope.launch {
            dao.resolveAlert(recordId, excuseType, excuseReason)
        }
    }
}
