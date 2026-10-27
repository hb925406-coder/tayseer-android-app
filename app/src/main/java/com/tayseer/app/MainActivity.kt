package com.tayseer.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.Student
import com.tayseer.app.ui.MainViewModel
import com.tayseer.app.util.StudentImporter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(database.appDao()) as T
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                    TayseerMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val students by viewModel.students.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val currentDate by viewModel.selectedDate.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentSection by remember { mutableStateOf("") }
    var newStudentPhone by remember { mutableStateOf("") }

    // مستورد ملفات الإكسل والـ CSV
    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val importedStudents = StudentImporter.importStudents(context, uri)
                    if (importedStudents.isNotEmpty()) {
                        viewModel.insertStudentsList(importedStudents)
                        Toast.makeText(context, "تم استيراد ${importedStudents.size} طالب بنجاح", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "لم يتم العثور على بيانات طلاب بالملف", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ أثناء قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // استخراج الحلقات/الأقسام المتوفرة
    val sections = remember(students) {
        listOf("الكل") + students.map { it.section }.filter { it.isNotBlank() }.distinct()
    }

    val filteredStudents = remember(students, selectedSection) {
        if (selectedSection == "الكل") students else students.filter { it.section == selectedSection }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تطبيق تيسير") },
                actions = {
                    TextButton(onClick = {
                        excelPickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/comma-separated-values",
                                "text/csv"
                            )
                        )
                    }) {
                        Text("استيراد Excel", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة طالب")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // شريط التاريخ والتحضير الجماعي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التاريخ: $currentDate",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = {
                        viewModel.markAllPresent(filteredStudents)
                        Toast.makeText(context, "تم تحضير جميع طلاب القائمة كـ حاضر", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحضير الكل حاضر", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // شريط تصفية الحلقات / الأقسام
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { sec ->
                    FilterChip(
                        selected = (sec == selectedSection),
                        onClick = { viewModel.setSection(sec) },
                        label = { Text(sec) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // قائمة الطلاب
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب. اضغط 'استيراد Excel' أو زر + للإضافة.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val attendance = attendanceRecords.find { it.studentId == student.id }
                        StudentRowCard(
                            student = student,
                            currentStatus = attendance?.status ?: "لم يُسجل",
                            onMarkAttendance = { status ->
                                viewModel.markAttendance(student.id, status)
                            },
                            onMarkEvaluation = { evalType ->
                                viewModel.markEvaluation(student.id, evalType)
                                Toast.makeText(context, "تم تسجيل: $evalType لـ ${student.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // نافذة إضافة طالب يدوي
        if (showAddStudentDialog) {
            AlertDialog(
                onDismissRequest = { showAddStudentDialog = false },
                title = { Text("إضافة طالب جديد") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newStudentName,
                            onValueChange = { newStudentName = it },
                            label = { Text("اسم الطالب رباعي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newStudentSection,
                            onValueChange = { newStudentSection = it },
                            label = { Text("الحلقة / الصف") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newStudentPhone,
                            onValueChange = { newStudentPhone = it },
                            label = { Text("رقم ولي الأمر") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newStudentName.isNotBlank()) {
                                viewModel.insertStudent(
                                    Student(
                                        name = newStudentName.trim(),
                                        section = newStudentSection.trim(),
                                        parentPhone = newStudentPhone.trim()
                                    )
                                )
                                newStudentName = ""
                                newStudentSection = ""
                                newStudentPhone = ""
                                showAddStudentDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStudentDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun StudentRowCard(
    student: Student,
    currentStatus: String,
    onMarkAttendance: (String) -> Unit,
    onMarkEvaluation: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // معلومات الطالب والحالة الحالية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                    if (student.section.isNotBlank()) {
                        Text(
                            text = "الحلقة: ${student.section}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                SuggestionChip(
                    onClick = { },
                    label = { Text(currentStatus) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when (currentStatus) {
                            "حاضر" -> MaterialTheme.colorScheme.primaryContainer
                            "غائب", "هارب" -> MaterialTheme.colorScheme.errorContainer
                            "متأخر", "مستأذن", "غائب بعذر" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الحالات الست الكاملة للحضور
            Text("تسجيل الحضور:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val statuses = listOf("حاضر", "غائب", "مستأذن", "متأخر", "غائب بعذر", "هارب")
                statuses.forEach { status ->
                    OutlinedButton(
                        onClick = { onMarkAttendance(status) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(status, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // بنود التقييم الخمسة
            Text("التقييم السريع:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val evals = listOf("حفظه", "لم يحفظ", "مميز", "مؤدب", "نسيان الكتب")
                evals.forEach { eval ->
                    FilledTonalButton(
                        onClick = { onMarkEvaluation(eval) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(eval, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
