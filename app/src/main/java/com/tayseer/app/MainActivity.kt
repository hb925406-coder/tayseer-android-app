package com.tayseer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.Student
import com.tayseer.app.ui.MainViewModel

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
                    TayseerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerApp(viewModel: MainViewModel) {
    val students by viewModel.students.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentSection by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تطبيق تيسير - الحضور والتقييم") },
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
            Text(
                text = "التاريخ الحالي: $selectedDate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب مسجلون حالياً. اضغط + لإضافة طالب.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students) { student ->
                        val attendance = attendanceRecords.find { it.studentId == student.id }
                        StudentItemCard(
                            student = student,
                            attendanceStatus = attendance?.status ?: "غير مسجل",
                            onMarkAttendance = { status ->
                                viewModel.markAttendance(student.id, status)
                            },
                            onMarkEvaluation = { type ->
                                viewModel.markEvaluation(student.id, type)
                            }
                        )
                    }
                }
            }
        }

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
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newStudentSection,
                            onValueChange = { newStudentSection = it },
                            label = { Text("الصف / الحلقة") },
                            singleLine = true
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
                                        section = newStudentSection.trim()
                                    )
                                )
                                newStudentName = ""
                                newStudentSection = ""
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
fun StudentItemCard(
    student: Student,
    attendanceStatus: String,
    onMarkAttendance: (String) -> Unit,
    onMarkEvaluation: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                    if (student.section.isNotBlank()) {
                        Text(
                            text = student.section,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = attendanceStatus,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (attendanceStatus) {
                        "حاضر" -> MaterialTheme.colorScheme.primary
                        "غائب" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // أزرار الحضور
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { onMarkAttendance("حاضر") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("حاضر", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onMarkAttendance("غائب") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("غائب", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onMarkAttendance("متأخر") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("متأخر", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // أزرار سريعة للتقييم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = { onMarkEvaluation("حفظه") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("حفظه", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = { onMarkEvaluation("لم يحفظ") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("لم يحفظ", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = { onMarkEvaluation("مميز") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text("مميز", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
