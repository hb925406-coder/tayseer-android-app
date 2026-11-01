package com.tayseer.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.CsvImporter
import com.tayseer.app.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val studentDao = AppDatabase.getDatabase(application).studentDao()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    init {
        loadStudents()
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

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }
}
