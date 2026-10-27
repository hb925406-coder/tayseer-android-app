package com.tayseer.app

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.data.User
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
                    val currentUser by viewModel.currentUser.collectAsState()

                    when {
                        currentUser == null -> LoginScreen(viewModel = viewModel)
                        currentUser!!.role == "PARENT" -> ParentDashboardScreen(viewModel = viewModel, user = currentUser!!)
                        else -> TayseerMainScreen(viewModel = viewModel, user = currentUser!!)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تطبيق تيسير",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "تسجيل الدخول المحلي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (loginError != null) {
                    Text(
                        text = loginError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دخول")
                }

                Text(
                    text = "الإدارة: admin / المعلم: teacher / ولي الأمر: parent\nكلمة المرور للجميع: 123456",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(viewModel: MainViewModel, user: User) {
    val parentStudent by viewModel.parentStudent.collectAsState()
    val attendanceList by viewModel.parentAttendance.collectAsState()
    val evaluationList by viewModel.parentEvaluations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("بوابة ولي الأمر")
                        Text(
                            text = "المستخدم: ${user.username}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.logout() }) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "بيانات الطالب",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "الاسم: ${parentStudent?.name ?: "جاري التحميل..."}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "الحلقة: ${parentStudent?.section ?: "غير محدد"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text("سجل الحضور الأخير", style = MaterialTheme.typography.titleSmall)
            if (attendanceList.isEmpty()) {
                Text("لا توجد سجلات حضور مسجلة حتى الآن", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(attendanceList) { record ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(record.date, style = MaterialTheme.typography.bodyMedium)
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(record.status) }
                                )
                            }
                        }
                    }
                }
            }

            Text("سجل التقييمات والسلوك", style = MaterialTheme.typography.titleSmall)
            if (evaluationList.isEmpty()) {
                Text("لا توجد تقييمات مسجلة حتى الآن", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(evaluationList) { eval ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(eval.date, style = MaterialTheme.typography.bodyMedium)
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(eval.evaluationType) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainScreen(viewModel: MainViewModel, user: User) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val students by viewModel.students.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()
    val displayDate by viewModel.displayDate.collectAsState()
    val isHijri by viewModel.isHijri.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    var newStudentName by remember { mutableStateOf("") }
    var newStudentSection by remember { mutableStateOf("") }
    var newStudentPhone by remember { mutableStateOf("") }

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

    val sections = remember(students) {
        listOf("الكل") + students.map { it.section }.filter { it.isNotBlank() }.distinct()
    }

    val filteredStudents = remember(students, selectedSection, searchQuery) {
        students.filter { student ->
            val matchSection = (selectedSection == "الكل" || student.section == selectedSection)
            val matchSearch = searchQuery.isBlank() || student.name.contains(searchQuery.trim(), ignoreCase = true)
            matchSection && matchSearch
        }
    }

    fun shareDailyReport() {
        val totalCount = filteredStudents.size
        val presentCount = attendanceRecords.count { it.status == "حاضر" }
        val absentCount = attendanceRecords.count { it.status == "غائب" }
        val lateCount = attendanceRecords.count { it.status == "متأخر" }
        val excusedCount = attendanceRecords.count { it.status == "مستأذن" || it.status == "غائب بعذر" }

        val reportText = buildString {
            appendLine("📊 *تقرير متابعة الطلاب - تطبيق تيسير*")
            appendLine("التاريخ: $displayDate")
            appendLine("الحلقة/القسم: $selectedSection")
            appendLine("---------------------------")
            appendLine("إجمالي الطلاب: $totalCount")
            appendLine("✅ الحضور: $presentCount")
            appendLine("❌ الغياب: $absentCount")
            appendLine("⏰ التأخر: $lateCount")
            appendLine("📝 بعذر/مستأذن: $excusedCount")
            appendLine("---------------------------")
            appendLine("كشف الحالات:")
            filteredStudents.forEachIndexed { index, student ->
                val record = attendanceRecords.find { it.studentId == student.id }
                val status = record?.status ?: "لم يُسجل"
                appendLine("${index + 1}. ${student.name} : $status")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة تقرير اليوم")
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("تطبيق تيسير")
                        Text(
                            text = "المستخدم: ${user.username} (${if (user.role == "ADMIN") "إدارة" else "معلم"})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareDailyReport() }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة التقرير")
                    }

                    IconButton(onClick = { showAlertsDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (pendingAlerts.isNotEmpty()) {
                                    Badge { Text("${pendingAlerts.size}") }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "التنبيهات")
                        }
                    }

                    if (user.role == "ADMIN") {
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
                    }

                    TextButton(onClick = { viewModel.logout() }) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (user.role == "ADMIN") {
                FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.toggleCalendarType() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (isHijri) "تحويل للميلادي" else "تحويل للهجري", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        viewModel.markAllPresent(filteredStudents)
                        Toast.makeText(context, "تم تحضير جميع طلاب القائمة كـ حاضر", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الكل حاضر", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن طالب بالاسم...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank()) "لا توجد نتائج تطابق: $searchQuery"
                        else "لا يوجد طلاب. اضغط 'استيراد Excel' أو زر + للإضافة."
                    )
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

        if (showAlertsDialog) {
            AlertDialog(
                onDismissRequest = { showAlertsDialog = false },
                title = { Text("تنبيهات الغياب والتأخر (${pendingAlerts.size})") },
                text = {
                    if (pendingAlerts.isEmpty()) {
                        Text("ممتاز! لا توجد حالات غياب أو تأخر تحتاج لمتابعة.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingAlerts, key = { it.id }) { alertRecord ->
                                val student = students.find { it.id == alertRecord.studentId }
                                AlertItemCard(
                                    studentName = student?.name ?: "طالب",
                                    record = alertRecord,
                                    onResolve = { excuseType, reason ->
                                        viewModel.resolveAlert(alertRecord.id, excuseType, reason)
                                        Toast.makeText(context, "تم توثيق العذر بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAlertsDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
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
fun AlertItemCard(
    studentName: String,
    record: AttendanceRecord,
    onResolve: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(studentName, style = MaterialTheme.typography.titleSmall)
                Text("${record.status} (${record.date})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("توثيق المتابعة بواسطة:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val excuseTypes = listOf("ورقة من ولي الأمر", "اتصال", "رسالة واتساب", "رسالة SMS")
                excuseTypes.forEach { type ->
                    OutlinedButton(
                        onClick = { onResolve(type, "تم التوثيق عبر $type") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(type, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
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
