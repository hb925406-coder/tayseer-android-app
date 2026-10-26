package com.tayseer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.Level
import com.tayseer.app.data.Student
import com.tayseer.app.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF006C4C),
                        onPrimary = Color.White,
                        secondary = Color(0xFF4C6358),
                        background = Color(0xFFFBFDF9),
                        surface = Color.White
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TayseerMainScreen(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerMainScreen(viewModel: MainViewModel) {
    val levels by viewModel.levels.collectAsState(initial = emptyList())
    val selectedLevelId by viewModel.selectedLevelId.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val unresolvedAlerts by viewModel.unresolvedAlerts.collectAsState(initial = emptyList())

    val students by remember(selectedLevelId) {
        if (selectedLevelId != null) {
            viewModel.getStudentsForCurrentLevel(selectedLevelId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val attendanceToday by viewModel.getAttendanceForDate(currentDate).collectAsState(initial = emptyList())

    var showAddLevelDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تطبيق تيسير",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unresolvedAlerts.isNotEmpty()) {
                                Badge { Text(unresolvedAlerts.size.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { showAlertsDialog = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (selectedLevelId != null) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة طالب", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // شريط التاريخ والإحصائيات
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التاريخ: $currentDate", fontWeight = FontWeight.Bold)
                    Text("الطلاب المسجلين: ${students.size}", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // قسم اختيار أو إضافة الحلقة / الفصل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الحلقة / الفصل:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { showAddLevelDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة حلقة")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (levels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد حلقات دراسية حالياً.\nاضغط 'إضافة حلقة' لبدء إضافة الطلاب وتسجيل الحضور.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                ScrollableTabRow(
                    selectedTabIndex = levels.indexOfFirst { it.id == selectedLevelId }.coerceAtLeast(0),
                    edgePadding = 0.dp
                ) {
                    levels.forEach { level ->
                        Tab(
                            selected = level.id == selectedLevelId,
                            onClick = { viewModel.selectLevel(level.id) },
                            text = { Text(level.name, fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                LaunchedEffect(levels) {
                    if (selectedLevelId == null && levels.isNotEmpty()) {
                        viewModel.selectLevel(levels.first().id)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // قائمة الطلاب والحضور
                if (students.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "لا يوجد طلاب في هذه الحلقة.\nاضغط على علامة (+) بالأسفل لإضافة طلاب.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(students) { student ->
                            val currentStatus = attendanceToday.find { it.studentId == student.id }?.status ?: "لم يسجل"
                            StudentAttendanceCard(
                                student = student,
                                currentStatus = currentStatus,
                                onSelectStatus = { status ->
                                    viewModel.recordAttendance(student.id, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة إضافة حلقة جديدة
    if (showAddLevelDialog) {
        var levelName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLevelDialog = false },
            title = { Text("إضافة حلقة / فصل دراسي") },
            text = {
                OutlinedTextField(
                    value = levelName,
                    onValueChange = { levelName = it },
                    label = { Text("اسم الحلقة أو الفصل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (levelName.isNotBlank()) {
                            viewModel.addLevel(levelName.trim())
                            showAddLevelDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLevelDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // نافذة إضافة طالب جديد
    if (showAddStudentDialog && selectedLevelId != null) {
        var studentName by remember { mutableStateOf("") }
        var parentPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("إضافة طالب جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("اسم الطالب الرباعي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("رقم هاتف ولي الأمر (واتساب)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studentName.isNotBlank()) {
                            viewModel.addStudent(
                                levelId = selectedLevelId!!,
                                name = studentName.trim(),
                                parentPhone = parentPhone.trim()
                            )
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

    // نافذة متابعة التنبيهات والأعذار
    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text("تنبيهات الغياب والتأخر التي تحتاج متابعة") },
            text = {
                if (unresolvedAlerts.isEmpty()) {
                    Text("رائع! لا توجد حالات غياب أو تأخر معلقة حالياً.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(unresolvedAlerts) { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("تاريخ الحالة: ${alert.date}", fontWeight = FontWeight.Bold)
                                    Text("الحالة: ${alert.status}", color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { viewModel.resolveAlert(alert, "ورقة عذر", "ورقة") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("عذر ورقي", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.resolveAlert(alert, "اتصال ولي الأمر", "اتصال") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("اتصال", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
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
}

@Composable
fun StudentAttendanceCard(
    student: Student,
    currentStatus: String,
    onSelectStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = currentStatus,
                    color = when (currentStatus) {
                        "حاضر" -> Color(0xFF00897B)
                        "غائب" -> MaterialTheme.colorScheme.error
                        "متأخر" -> Color(0xFFF57C00)
                        "مستأذن" -> Color(0xFF1976D2)
                        "غائب بعذر" -> Color(0xFF7B1FA2)
                        "هارب" -> Color(0xFFC2185B)
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الحالات الست للحضور
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceOptionButton(title = "حاضر", isSelected = currentStatus == "حاضر", color = Color(0xFF00897B), modifier = Modifier.weight(1f)) { onSelectStatus("حاضر") }
                AttendanceOptionButton(title = "غائب", isSelected = currentStatus == "غائب", color = Color(0xFFD32F2F), modifier = Modifier.weight(1f)) { onSelectStatus("غائب") }
                AttendanceOptionButton(title = "مستأذن", isSelected = currentStatus == "مستأذن", color = Color(0xFF1976D2), modifier = Modifier.weight(1f)) { onSelectStatus("مستأذن") }
                AttendanceOptionButton(title = "متأخر", isSelected = currentStatus == "متأخر", color = Color(0xFFF57C00), modifier = Modifier.weight(1f)) { onSelectStatus("متأخر") }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceOptionButton(title = "غائب بعذر", isSelected = currentStatus == "غائب بعذر", color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f)) { onSelectStatus("غائب بعذر") }
                AttendanceOptionButton(title = "هارب", isSelected = currentStatus == "هارب", color = Color(0xFFC2185B), modifier = Modifier.weight(1f)) { onSelectStatus("هارب") }
            }
        }
    }
}

@Composable
fun AttendanceOptionButton(
    title: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.12f),
            contentColor = if (isSelected) Color.White else color
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
