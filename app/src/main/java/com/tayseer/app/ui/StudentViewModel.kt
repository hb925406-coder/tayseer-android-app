package com.tayseer.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.CsvImporter
import com.tayseer.app.data.CustomEvaluation
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.util.BackupManager
import com.tayseer.app.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()
    private val appDao = database.appDao()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    // متغيرات البحث وتصفية الشعب والحلقات
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSection = MutableStateFlow("الكل")
    val selectedSection: StateFlow<String> = _selectedSection.asStateFlow()

    // قائمة الطلاب المفلترة تلقائياً بحسب البحث والشعبة
    val filteredStudents: StateFlow<List<Student>> = combine(
        _students,
        _searchQuery,
        _selectedSection
    ) { studentList, query, section ->
        studentList.filter { student ->
            val matchesQuery = query.isBlank() || student.name.contains(query.trim(), ignoreCase = true)
            val matchesSection = section == "الكل" || student.section == section
            matchesQuery && matchesSection
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // قائمة أسماء الحلقات/الشعب المتاحة
    val availableSections: StateFlow<List<String>> = _students.flatMapLatest { list ->
        val sections = listOf("الكل") + list.map { it.section }.distinct().filter { it.isNotBlank() }
        MutableStateFlow(sections)
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("الكل"))

    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayAttendance: StateFlow<List<AttendanceRecord>> = _currentDate.flatMapLatest { date ->
        attendanceDao.getAttendanceByDate(date)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingAlerts: StateFlow<List<AttendanceRecord>> = attendanceDao.getPendingFollowUps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customEvaluations: StateFlow<List<CustomEvaluation>> = appDao.getAllCustomEvaluations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSectionSelect(section: String) {
        _selectedSection.value = section
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
            val targetList = filteredStudents.value.ifEmpty { _students.value }
            if (targetList.isNotEmpty()) {
                val records = targetList.map { student ->
                    AttendanceRecord(
                        studentId = student.id,
                        date = date,
                        status = AttendanceStatus.PRESENT.name,
                        isResolved = true
                    )
                }
                attendanceDao.recordBulkAttendance(records)
                _importStatusMessage.value = "تم تحضير (${targetList.size}) طالب كـ (حاضر)"
            }
        }
    }

    fun updateStudentAttendance(studentId: Long, status: AttendanceStatus, note: String = "") {
        viewModelScope.launch {
            val isResolvedState = (status == AttendanceStatus.PRESENT)
            val record = AttendanceRecord(
                studentId = studentId,
                date = _currentDate.value,
                status = status.name,
                reasonNote = note,
                isResolved = isResolvedState
            )
            attendanceDao.recordAttendance(record)
        }
    }

    fun resolveAlert(recordId: Long, method: String, note: String) {
        viewModelScope.launch {
            attendanceDao.resolveFollowUp(recordId, method, note)
            _importStatusMessage.value = "تم توثيق العذر وإغلاق التنبيه بنجاح"
        }
    }

    fun addEvaluation(studentId: Long, evaluationType: String, note: String = "") {
        viewModelScope.launch {
            val record = EvaluationRecord(
                studentId = studentId,
                date = _currentDate.value,
                evaluationType = evaluationType,
                note = note
            )
            appDao.insertEvaluation(record)
            _importStatusMessage.value = "تم تسجيل تقييم ($evaluationType) بنجاح"
        }
    }

    fun addCustomEvaluation(title: String, isPositive: Boolean) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                appDao.insertCustomEvaluation(
                    CustomEvaluation(
                        title = title.trim(),
                        isPositive = isPositive
                    )
                )
            }
        }
    }

    fun deleteCustomEvaluation(evaluation: CustomEvaluation) {
        viewModelScope.launch {
            appDao.deleteCustomEvaluation(evaluation)
        }
    }

    fun getStudentEvaluations(studentId: Long) = appDao.getEvaluationsForStudent(studentId)

    // تصدير ومشاركة تقرير كشف الحضور اليومي بصيغة PDF
    fun exportAndShareDailyReport(context: Context, formattedDateDisplay: String) {
        viewModelScope.launch {
            try {
                val currentStudents = filteredStudents.value.ifEmpty { _students.value }
                val currentRecords = todayAttendance.value
                if (currentStudents.isEmpty()) {
                    _importStatusMessage.value = "لا يوجد طلاب لإنشاء التقرير"
                    return@launch
                }

                val sectionTitle = if (_selectedSection.value == "الكل") "جميع الحلقات" else _selectedSection.value

                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateDailyReportPdf(
                        context = context,
                        students = currentStudents,
                        records = currentRecords,
                        dateString = formattedDateDisplay,
                        section = sectionTitle
                    )
                }

                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "كشف حضور تيسير - $formattedDateDisplay")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير تيسير عبر:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                _importStatusMessage.value = "تعذر إنشاء ملف التقرير: ${e.localizedMessage}"
            }
        }
    }

    // تصدير ومشاركة خلاصة الطالب المفصلة بصيغة PDF
    fun exportAndShareStudentSummary(context: Context, student: Student) {
        viewModelScope.launch {
            try {
                val evals = withContext(Dispatchers.IO) {
                    appDao.getEvaluationsForStudent(student.id).first()
                }
                val attendanceHistory = withContext(Dispatchers.IO) {
                    attendanceDao.getAttendanceForStudent(student.id).first()
                }

                val pdfFile = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateStudentSummaryPdf(
                        context = context,
                        student = student,
                        attendanceList = attendanceHistory,
                        evaluationList = evals
                    )
                }

                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "تقرير خلاصة الطالب: ${student.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "مشاركة خلاصة الطالب عبر:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                _importStatusMessage.value = "تعذر إنشاء خلاصة الطالب: ${e.localizedMessage}"
            }
        }
    }

    // تصدير نسخة احتياطية من قاعدة البيانات ومشاركتها
    fun backupDatabase(context: Context) {
        viewModelScope.launch {
            val result = BackupManager.createBackupFile(context)
            result.onSuccess { file ->
                try {
                    val fileUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق تيسير")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "حفظ أو إرسال النسخة الاحتياطية عبر:")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    _importStatusMessage.value = "تم تجهيز النسخة الاحتياطية بنجاح"
                } catch (e: Exception) {
                    _importStatusMessage.value = "تعذر مشاركة النسخة الاحتياطية: ${e.localizedMessage}"
                }
            }.onFailure { error ->
                _importStatusMessage.value = "فشل إنشاء النسخة الاحتياطية: ${error.localizedMessage}"
            }
        }
    }

    // استعادة قاعدة البيانات من ملف نسخة احتياطية
    fun restoreDatabase(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            val result = BackupManager.restoreDatabaseFromUri(context, fileUri)
            result.onSuccess {
                _importStatusMessage.value = "تمت استعادة البيانات بنجاح! يُفضل إعادة تشغيل التطبيق"
                loadStudents()
            }.onFailure { error ->
                _importStatusMessage.value = "فشل استعادة البيانات: ${error.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }
}
