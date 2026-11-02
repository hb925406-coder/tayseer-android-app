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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.EvaluationRecord
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
                    TayseerMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainApp(viewModel: StudentViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val statusMessage by viewModel.importStatusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "الحضور والغياب") },
                    label = { Text("الحضور والغياب") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "التقييم والسلوك") },
                    label = { Text("التقييم والسلوك") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                AttendanceScreen(viewModel = viewModel)
            } else {
                EvaluationsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: StudentViewModel) {
    val students by viewModel.students.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var studentNameInput by remember { mutableStateOf("") }
    var studentSectionInput by remember { mutableStateOf("") }

    var selectedStudentForAttendance by remember { mutableStateOf<Student?>(null) }
    var attendanceNoteInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importStudentsFromCsv(it) }
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
                        Icon(Icons.Default.DoneAll, contentDescription = "تحضير الكل كحاضر")
                    }
                    IconButton(onClick = {
                        filePickerLauncher.launch(arrayOf("text/*", "application/*"))
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "استيراد ملف")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
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
                    Text("لا يوجد طلاب مسجلون حالياً. استخدم زر + لإضافة طالب.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "الحلقة/الشعبة: ${student.section}", style = MaterialTheme.typography.bodySmall)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationsScreen(viewModel: StudentViewModel) {
    val students by viewModel.students.collectAsState()
    val customEvaluations by viewModel.customEvaluations.collectAsState()

    var selectedStudentForEval by remember { mutableStateOf<Student?>(null) }
    var selectedStudentForHistory by remember { mutableStateOf<Student?>(null) }
    var evalNoteInput by remember { mutableStateOf("") }
    var showCustomEvalManager by remember { mutableStateOf(false) }
    var newCustomEvalTitle by remember { mutableStateOf("") }

    val defaultEvals = listOf("حفظه", "لم يحفظ", "مميز", "مؤدب", "نسيان الكتب")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تيسير - التقييم والسلوك") },
                actions = {
                    IconButton(onClick = { showCustomEvalManager = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "إدارة التقييمات المخصصة")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "اختر طالباً لتسجيل التقييم أو استعراض سجله:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب مسجلون حالياً.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(students) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStudentForEval = student
                                    evalNoteInput = ""
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
                                    Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "الحلقة/الشعبة: ${student.section}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(onClick = { selectedStudentForHistory = student }) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "سجل التقييمات",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(onClick = {
                                        selectedStudentForEval = student
                                        evalNoteInput = ""
                                    }) {
                                        Text("تقييم")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // نافذة تسجيل تقييم للطالب
        selectedStudentForEval?.let { student ->
            AlertDialog(
                onDismissRequest = { selectedStudentForEval = null },
                title = { Text("تقييم الطالب: ${student.name}") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("التقييمات الأساسية:")
                        defaultEvals.forEach { evalType ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.addEvaluation(
                                        studentId = student.id,
                                        evaluationType = evalType,
                                        note = evalNoteInput.trim()
                                    )
                                    selectedStudentForEval = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(evalType)
                            }
                        }

                        if (customEvaluations.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("التقييمات المخصصة:")
                            customEvaluations.forEach { customEval ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.addEvaluation(
                                            studentId = student.id,
                                            evaluationType = customEval.title,
                                            note = evalNoteInput.trim()
                                        )
                                        selectedStudentForEval = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(customEval.title)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = evalNoteInput,
                            onValueChange = { evalNoteInput = it },
                            label = { Text("ملاحظة إضافية (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedStudentForEval = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة عرض سجل تقييمات الطالب السابق
        selectedStudentForHistory?.let { student ->
            val historyFlow = remember(student.id) { viewModel.getStudentEvaluations(student.id) }
            val historyList by historyFlow.collectAsState(initial = emptyList())

            AlertDialog(
                onDismissRequest = { selectedStudentForHistory = null },
                title = { Text("سجل تقييمات: ${student.name}") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد تقييمات مسجلة لهذا الطالب بعد.")
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(historyList) { record ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = record.evaluationType,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = record.date,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            if (!record.note.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "ملاحظة: ${record.note}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedStudentForHistory = null }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // نافذة إدارة التقييمات المخصصة
        if (showCustomEvalManager) {
            AlertDialog(
                onDismissRequest = { showCustomEvalManager = false },
                title = { Text("إدارة التقييمات المخصصة") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCustomEvalTitle,
                                onValueChange = { newCustomEvalTitle = it },
                                label = { Text("اسم التقييم الجديد") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newCustomEvalTitle.isNotBlank()) {
                                        viewModel.addCustomEvaluation(newCustomEvalTitle.trim(), isPositive = true)
                                        newCustomEvalTitle = ""
                                    }
                                }
                            ) {
                                Text("إضافة")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        if (customEvaluations.isEmpty()) {
                            Text("لا توجد تقييمات مخصصة بعد.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(customEvaluations) { customEval ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = customEval.title)
                                        IconButton(onClick = { viewModel.deleteCustomEvaluation(customEval) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomEvalManager = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}
