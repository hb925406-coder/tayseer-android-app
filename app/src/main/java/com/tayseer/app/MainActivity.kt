package com.tayseer.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.Student
import com.tayseer.app.ui.StudentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(viewModel: StudentViewModel) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val statusMessage by viewModel.importStatusMessage.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var studentNameInput by remember { mutableStateOf("") }
    var studentSectionInput by remember { mutableStateOf("") }

    var selectedStudentForAttendance by remember { mutableStateOf<Student?>(null) }
    var attendanceNoteInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importStudentsFromCsv(it)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    val attendanceMap = remember(todayAttendance) {
        todayAttendance.associateBy { it.studentId }
    }

    val presentCount = todayAttendance.count { it.statusEnum == AttendanceStatus.PRESENT }
    val absentCount = todayAttendance.count { 
        it.statusEnum == AttendanceStatus.ABSENT || it.statusEnum == AttendanceStatus.ABSENT_WITH_EXCUSE 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تيسير - متابعة الحضور") },
                actions = {
                    IconButton(onClick = { viewModel.setAllPresentForToday() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "تحضير الكل كحاضر"
                        )
                    }
                    IconButton(onClick = {
                        filePickerLauncher.launch(arrayOf("text/*", "application/*"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "استيراد ملف"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة طالب")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "التاريخ: $currentDate", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "حاضر: $presentCount | غائب: $absentCount", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(
                text = "قائمة الطلاب (الإجمالي: ${students.size}) - اضغط على الطالب لتسجيل الحالة:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب مسجلون حالياً. استخدم زر الاستيراد أو زر + لإضافة طلاب.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students) { student ->
                        val record = attendanceMap[student.id]
                        val currentStatus = record?.statusEnum ?: AttendanceStatus.PRESENT
                        val statusColor = when (currentStatus) {
                            AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                            AttendanceStatus.ABSENT, AttendanceStatus.TRUANT -> Color(0xFFC62828)
                            AttendanceStatus.LATE -> Color(0xFFEF6C00)
                            AttendanceStatus.EXCUSED, AttendanceStatus.ABSENT_WITH_EXCUSE -> Color(0xFF0277BD)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStudentForAttendance = student
                                    attendanceNoteInput = record?.reasonNote.orEmpty()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "الحلقة/الشعبة: ${student.section}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (!record?.reasonNote.isNullOrBlank()) {
                                        Text(
                                            text = "ملاحظة: ${record?.reasonNote}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                SuggestionChip(
                                    onClick = {
                                        selectedStudentForAttendance = student
                                        attendanceNoteInput = record?.reasonNote.orEmpty()
                                    },
                                    label = {
                                        Text(
                                            text = currentStatus.arabicLabel,
                                            color = statusColor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // نافذة اختيار حالة الحضور للطالب
        selectedStudentForAttendance?.let { student ->
            AlertDialog(
                onDismissRequest = { selectedStudentForAttendance = null },
                title = { Text("تسجيل حضور: ${student.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("اختر حالة الحضور:")
                        AttendanceStatus.values().forEach { status ->
                            Button(
                                onClick = {
                                    viewModel.updateStudentAttendance(
                                        studentId = student.id,
                                        status = status,
                                        note = attendanceNoteInput.trim()
                                    )
                                    selectedStudentForAttendance = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(status.arabicLabel)
                            }
                        }
                        OutlinedTextField(
                            value = attendanceNoteInput,
                            onValueChange = { attendanceNoteInput = it },
                            label = { Text("سبب الغياب / التأخير (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedStudentForAttendance = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة إضافة طالب يدوي
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("إضافة طالب جديد") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = studentNameInput,
                            onValueChange = { studentNameInput = it },
                            label = { Text("اسم الطالب") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = studentSectionInput,
                            onValueChange = { studentSectionInput = it },
                            label = { Text("الشعبة / الحلقة") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (studentNameInput.isNotBlank()) {
                                viewModel.addStudent(
                                    Student(
                                        name = studentNameInput.trim(),
                                        section = studentSectionInput.trim().ifEmpty { "عام" }
                                    )
                                )
                                studentNameInput = ""
                                studentSectionInput = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
