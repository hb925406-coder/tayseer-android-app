package com.tayseer.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.CustomEvaluation
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.util.DateUtils
import com.tayseer.app.util.PdfReportGenerator
import com.tayseer.app.util.StudentImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class UserRole(val title: String) {
    ADMIN("الإدارة"),
    TEACHER("المعلم"),
    PARENT("ولي الأمر")
}

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()
    private val evaluationDao = db.evaluationDao()

    private val _currentUserRole = MutableStateFlow(UserRole.ADMIN)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _currentAcademicYear = MutableStateFlow("1447-1448")
    val currentAcademicYear: StateFlow<String> = _currentAcademicYear.asStateFlow()

    private val _currentDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSection = MutableStateFlow("الكل")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    // جلب الطلاب النشطين فقط تلقائياً
    val students: StateFlow<List<Student>> = studentDao.getAllActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableSections: StateFlow<List<String>> = studentDao.getActiveSections()
        .map { sections -> listOf("الكل") + sections }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("الكل"))

    val filteredStudents: StateFlow<List<Student>> = combine(
        students,
        _searchQuery,
        _selectedSection
    ) { list, query, section ->
        list.filter { student ->
            val matchesQuery = student.name.contains(query, ignoreCase = true)
            val matchesSection = if (section == "الكل") true else student.section == section
            matchesQuery && matchesSection
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<AttendanceRecord>> = _currentDate.flatMapLatest { date ->
        attendanceDao.getAttendanceByDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAlerts: Flow<List<AttendanceRecord>> = attendanceDao.getPendingAlerts()

    val customEvaluations: StateFlow<List<CustomEvaluation>> = evaluationDao.getAllCustomEvaluations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUserRole(role: UserRole) {
        _currentUserRole.value = role
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSectionSelect(section: String) {
        _selectedSection.value = section
    }

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }

    fun addStudent(student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            val studentWithYear = if (student.academicYear.isBlank()) {
                student.copy(academicYear = _currentAcademicYear.value)
            } else student
            studentDao.insertStudent(studentWithYear)
        }
    }

    fun updateStudentAttendance(studentId: Long, status: AttendanceStatus, note: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _currentDate.value
            val existing = attendanceDao.getRecordForStudent(studentId, date)
            val record = existing?.copy(
                status = status.name,
                reasonNote = note
            ) ?: AttendanceRecord(
                studentId = studentId,
                date = date,
                status = status.name,
                reasonNote = note
            )
            attendanceDao.insertAttendance(record)
        }
    }

    fun setAllPresentForToday() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = filteredStudents.value
            val date = _currentDate.value
            val records = currentList.map { student ->
                val existing = attendanceDao.getRecordForStudent(student.id, date)
                existing?.copy(status = AttendanceStatus.PRESENT.name)
                    ?: AttendanceRecord(
                        studentId = student.id,
                        date = date,
                        status = AttendanceStatus.PRESENT.name
                    )
            }
            attendanceDao.insertAttendanceList(records)
        }
    }

    fun resolveAlert(recordId: Long, followUpMethod: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceDao.resolveFollowUp(recordId, followUpMethod, note)
        }
    }

    fun addEvaluation(studentId: Long, evaluationType: String, note: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _currentDate.value
            evaluationDao.insertEvaluation(
                EvaluationRecord(
                    studentId = studentId,
                    date = date,
                    evaluationType = evaluationType,
                    note = note
                )
            )
        }
    }

    fun getStudentEvaluations(studentId: Long): Flow<List<EvaluationRecord>> {
        return evaluationDao.getEvaluationsForStudent(studentId)
    }

    fun addCustomEvaluation(title: String, isPositive: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.insertCustomEvaluation(
                CustomEvaluation(title = title, isPositive = isPositive)
            )
        }
    }

    fun deleteCustomEvaluation(customEvaluation: CustomEvaluation) {
        viewModelScope.launch(Dispatchers.IO) {
            evaluationDao.deleteCustomEvaluation(customEvaluation)
        }
    }

    // بدء سنة دراسية جديدة مع أرشفة طلاب وسجلات السنة السابقة بأمان
    fun startNewAcademicYear(newYearName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldYear = _currentAcademicYear.value
            studentDao.archiveCurrentYearStudents(oldYear)
            _currentAcademicYear.value = newYearName
            _importStatusMessage.value = "تمت أرشفة سنة $oldYear وبدء السنة الدراسية الجديدة $newYearName بنجاح"
        }
    }

    fun importStudentsFromCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _importStatusMessage.value = "تعذر فتح الملف المحدد"
                    return@launch
                }
                val studentsList = StudentImporter.parseCsv(inputStream)
                if (studentsList.isNotEmpty()) {
                    val currentYear = _currentAcademicYear.value
                    val formattedList = studentsList.map { it.copy(academicYear = currentYear) }
                    studentDao.insertStudents(formattedList)
                    _importStatusMessage.value = "تم استيراد ${formattedList.size} طالب بنجاح للسنة الدراسية $currentYear"
                } else {
                    _importStatusMessage.value = "الملف فارغ أو لا يحتوي على تنسيق متوافق"
                }
            } catch (e: Exception) {
                _importStatusMessage.value = "حدث خطأ أثناء الاستيراد: ${e.localizedMessage}"
            }
        }
    }

    fun exportAndShareDailyReport(context: Context, displayDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = filteredStudents.value
                val date = _currentDate.value
                val attendance = attendanceDao.getAttendanceListByDateSync(date)
                val attendanceMap = attendance.associateBy { it.studentId }

                val file = PdfReportGenerator.generateDailyAttendancePdf(
                    context = context,
                    dateString = displayDate,
                    students = currentList,
                    attendanceMap = attendanceMap
                )
                PdfReportGenerator.sharePdfFile(context, file)
            } catch (e: Exception) {
                _importStatusMessage.value = "تعذر إنشاء تقرير PDF: ${e.localizedMessage}"
            }
        }
    }

    fun exportAndShareStudentSummary(context: Context, student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val evaluations = evaluationDao.getEvaluationsForStudentSync(student.id)
                val attendances = attendanceDao.getAttendanceForStudentSync(student.id)

                val file = PdfReportGenerator.generateStudentSummaryPdf(
                    context = context,
                    student = student,
                    evaluations = evaluations,
                    attendances = attendances
                )
                PdfReportGenerator.sharePdfFile(context, file)
            } catch (e: Exception) {
                _importStatusMessage.value = "تعذر إنشاء خلاصة الطالب: ${e.localizedMessage}"
            }
        }
    }

    fun backupDatabase(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("tayseer_database")
                if (!dbFile.exists()) {
                    _importStatusMessage.value = "قاعدة البيانات فارغة أو غير موجودة بعد"
                    return@launch
                }
                val backupDir = File(context.getExternalFilesDir(null), "TayseerBackups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "tayseer_backup_$timeStamp.db")

                FileInputStream(dbFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.copyTo(output)
                    }
                }
                _importStatusMessage.value = "تم إنشاء النسخة الاحتياطية بنجاح في مجلد التطبيق"
            } catch (e: Exception) {
                _importStatusMessage.value = "فشل إنشاء النسخة الاحتياطية: ${e.localizedMessage}"
            }
        }
    }

    fun restoreDatabase(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("tayseer_database")
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _importStatusMessage.value = "تعذر قراءة ملف النسخة الاحتياطية"
                    return@launch
                }
                FileOutputStream(dbFile).use { output ->
                    inputStream.copyTo(output)
                }
                _importStatusMessage.value = "تمت استعادة البيانات بنجاح! يرجى إعادة فتح التطبيق لتحديث السجلات"
            } catch (e: Exception) {
                _importStatusMessage.value = "فشل استعادة البيانات: ${e.localizedMessage}"
            }
        }
    }
}
