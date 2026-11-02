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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.ui.FollowUpScreen
import com.tayseer.app.ui.StudentViewModel
import com.tayseer.app.ui.UserRole
import com.tayseer.app.ui.theme.TayseerTheme
import com.tayseer.app.util.DateUtils

class MainActivity : ComponentActivity() {

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDark) }

            TayseerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TayseerMainApp(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainApp(
    viewModel: StudentViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val statusMessage by viewModel.importStatusMessage.collectAsState()

    val currentRole by viewModel.currentUserRole.collectAsState()
    val pendingAlerts by viewModel.pendingAlerts.collectAsState(initial = emptyList())
    val students by viewModel.students.collectAsState(initial = emptyList())
    val studentsMap = remember(students) { students.associateBy { it.id } }

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
                if (currentRole != UserRole.PARENT) {
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingAlerts.isNotEmpty()) {
                                        Badge { Text("${pendingAlerts.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "التنبيهات والمتابعة")
                            }
                        },
                        label = { Text("المتابعة") }
                    )
                }
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "التقييم والسلوك") },
                    label = { Text("التقييم والسلوك") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> AttendanceScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
                1 -> {
                    if (currentRole != UserRole.PARENT) {
                        FollowUpScreen(
                            pendingRecords = pendingAlerts,
                            studentsMap = studentsMap,
                            onResolveFollowUp = { recordId, method, note ->
                                viewModel.resolveAlert(recordId, method, note)
                            }
                        )
                    } else {
                        AttendanceScreen(
                            viewModel = viewModel,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode
                        )
                    }
                }
                2 -> EvaluationsScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: StudentViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentUserRole.collectAsState()
    val filteredStudents by viewModel.filteredStudents.collectAsState()
    val availableSections by viewModel.availableSections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    var showHijri by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

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

    val restoreDbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreDatabase(context, it) }
    }

    val attendanceMap = remember(todayAttendance) {
        todayAttendance.associateBy { it.studentId }
    }

    val presentCount = todayAttendance.count { it.statusEnum == AttendanceStatus.PRESENT }
    val absentCount = todayAttendance.count { 
        it.statusEnum == AttendanceStatus.ABSENT || it.statusEnum == AttendanceStatus.ABSENT_WITH_EXCUSE 
    }

    val displayDate = if (showHijri) DateUtils.formatToHijri(currentDate) else DateUtils.formatGregorianArabic(currentDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("تيسير - متابعة الحضور", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("الوضع: ${currentRole.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    // زر التبديل المباشر بين الوضع الليلي والوضع الفاتح
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "تفعيل الوضع الفاتح" else "تفعيل الوضع الليلي",
                            tint = if (isDarkMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // زر اختيار وتبديل الدور
                    IconButton(onClick = { showRoleDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "تبديل الدور")
                    }

                    if (currentRole != UserRole.PARENT) {
                        IconButton(onClick = { viewModel.setAllPresentForToday() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "تحضير الكل كحاضر")
                        }
                    }

                    IconButton(onClick = {
                        viewModel.exportAndShareDailyReport(context, displayDate)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "مشاركة تقرير PDF اليومي", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (currentRole == UserRole.ADMIN) {
                        IconButton(onClick = {
                            filePickerLauncher.launch(arrayOf("text/*", "application/*"))
                        }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "استيراد ملف")
                        }
                    }

                    if (currentRole == UserRole.ADMIN) {
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "المزيد من الخيارات")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("إنشاء نسخة احتياطية") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.backupDatabase(context)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Backup, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("استعادة نسخة احتياطية") },
                                    onClick = {
                                        showMenu = false
                                        restoreDbPickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRole == UserRole.ADMIN) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("بحث عن اسم الطالب...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableSections.forEach { sectionName ->
                    FilterChip(
                        selected = (selectedSection == sectionName),
                        onClick = { viewModel.onSectionSelect(sectionName) },
                        label = { Text(sectionName) }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayDate,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        TextButton(
                            onClick = { showHijri = !showHijri },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showHijri) "عرض الميلادي" else "عرض الهجري",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "حاضر: $presentCount طالب",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "غائب: $absentCount طالب",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "المعروض: ${filteredStudents.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = if (currentRole != UserRole.PARENT) "قائمة الطلاب - اضغط على الطالب لتسجيل الحالة:" else "قائمة الطلاب (وضع العرض):",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotEmpty() || selectedSection != "الكل")
                            "لا توجد نتائج مطابقة للبحث أو الحلقة المحددة."
                        else
                            "لا يوجد طلاب مسجلون حالياً."
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredStudents) { student ->
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
                                .clickable(enabled = (currentRole != UserRole.PARENT)) {
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
                                        if (currentRole != UserRole.PARENT) {
                                            selectedStudentForAttendance = student
                                            attendanceNoteInput = record?.reasonNote.orEmpty()
                                        }
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

        if (showRoleDialog) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = { Text("اختر دور المستخدم الحالي") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.values().forEach { role ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.setUserRole(role)
                                    showRoleDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (currentRole == role) "✔ ${role.title}" else role.title,
                                    fontWeight = if (currentRole == role) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRoleDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
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
fun EvaluationsScreen(
    viewModel: StudentViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentUserRole.collectAsState()
    val filteredStudents by viewModel.filteredStudents.collectAsState()
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
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "تفعيل الوضع الفاتح" else "تفعيل الوضع الليلي",
                            tint = if (isDarkMode) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (currentRole == UserRole.ADMIN) {
                        IconButton(onClick = { showCustomEvalManager = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "إدارة التقييمات المخصصة")
                        }
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
                text = if (currentRole != UserRole.PARENT) "اختر طالباً لتسجيل التقييم أو استعراض سجله:" else "سجل تقييمات الطلاب:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب مسجلون حالياً.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredStudents) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = (currentRole != UserRole.PARENT)) {
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
                                    if (currentRole != UserRole.PARENT) {
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
        }

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

        selectedStudentForHistory?.let { student ->
            val historyFlow = remember(student.id) { viewModel.getStudentEvaluations(student.id) }
            val historyList by historyFlow.collectAsState(initial = emptyList())

            AlertDialog(
                onDismissRequest = { selectedStudentForHistory = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("سجل الطالب: ${student.name}", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            viewModel.exportAndShareStudentSummary(context, student)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة خلاصة الطالب PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
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
                    Button(
                        onClick = {
                            viewModel.exportAndShareStudentSummary(context, student)
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير خلاصة الطالب PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedStudentForHistory = null }) {
                        Text("إغلاق")
                    }
                }
            )
        }

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
