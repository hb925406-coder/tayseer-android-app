package com.tayseer.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(database.appDao()) as T
            }
        }
        setContent {
            MaterialTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                TayseerApp(viewModel)
            }
        }
    }
}

@Composable
fun TayseerApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser == null) {
        LoginScreen(onLogin = { username, password ->
            viewModel.login(username, password) { success -> }
        })
    } else {
        MainDashboard(viewModel)
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تطبيق تيسير", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("نظام إدارة الحضور والتقييم الذكي", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("اسم المستخدم") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    onLogin(username, password)
                } else {
                    Toast.makeText(context, "يرجى إدخال اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("تسجيل الدخول", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(0) }
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()
    var showAddStudentDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingAlerts.isNotEmpty()) {
                                Badge { Text(pendingAlerts.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                        }
                    },
                    label = { Text("التنبيهات") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                0 -> HomeScreen(viewModel)
                1 -> AlertsScreen(viewModel)
            }

            if (showAddStudentDialog) {
                AddStudentDialog(
                    onDismiss = { showAddStudentDialog = false },
                    onConfirm = { name, section, phone ->
                        viewModel.addStudent(name, section, phone)
                        showAddStudentDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val students by viewModel.activeStudents.collectAsState()
    val isHijri by viewModel.isHijri.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    var evaluationStudent by remember { mutableStateOf<Student?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // الشريط العلوي والتقويم
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("نظام تيسير", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = if (isHijri) "التاريخ (هجري): $selectedDate" else "التاريخ (ميلادي): $selectedDate",
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = { viewModel.toggleCalendarType() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "تبديل التقويم")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // زر تحضير الكل حاضر
        Button(
            onClick = {
                viewModel.markAllPresent()
                Toast.makeText(context, "تم تحضير جميع الطلاب كـ حاضر", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.DoneAll, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تحضير الكل حاضر")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // قائمة الطلاب
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد طلاب مسجلون حالياً. اضغط (+) لإضافة طالب.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students) { student ->
                    StudentItemCard(
                        student = student,
                        onAttendance = { status ->
                            viewModel.setAttendance(student.id, status)
                            Toast.makeText(context, "تم تسجيل: $status للطالب ${student.name}", Toast.LENGTH_SHORT).show()
                        },
                        onOpenEvaluation = { evaluationStudent = student }
                    )
                }
            }
        }
    }

    evaluationStudent?.let { student ->
        EvaluationDialog(
            student = student,
            viewModel = viewModel,
            onDismiss = { evaluationStudent = null }
        )
    }
}

@Composable
fun StudentItemCard(
    student: Student,
    onAttendance: (String) -> Unit,
    onOpenEvaluation: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("الشعبة: ${student.section}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onOpenEvaluation) {
                    Icon(Icons.Default.Star, contentDescription = "تقييم", tint = Color(0xFFFFB300))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statuses = listOf("حاضر", "غائب", "مستأذن", "متأخر", "غائب بعذر", "هارب")
                statuses.forEach { status ->
                    AssistChip(
                        onClick = { onAttendance(status) },
                        label = { Text(status, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun EvaluationDialog(
    student: Student,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val customEvals by viewModel.customEvaluations.collectAsState()
    val context = LocalContext.current
    var showAddCustom by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقييم الطالب: ${student.name}") },
        text = {
            Column {
                Text("التقييمات الأساسية:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("حفظه", "لم يحفظ", "مميز", "مؤدب", "نسيان الكتب").forEach { item ->
                        FilledTonalButton(onClick = {
                            viewModel.addEvaluation(student.id, item)
                            Toast.makeText(context, "تم تسجيل: $item", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }) {
                            Text(item, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تقييمات مخصصة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { showAddCustom = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة تقييم مخصص")
                    }
                }
                if (showAddCustom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            placeholder = { Text("اسم التقييم") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (customTitle.isNotBlank()) {
                                viewModel.addCustomEvaluation(customTitle)
                                customTitle = ""
                                showAddCustom = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "حفظ")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                customEvals.forEach { custom ->
                    OutlinedButton(
                        onClick = {
                            viewModel.addEvaluation(student.id, custom.title)
                            Toast.makeText(context, "تم تسجيل: ${custom.title}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(custom.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alerts by viewModel.pendingAlerts.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("حالات الغياب والتأخير التي تتطلب متابعة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد حالات غياب أو تأخير تتطلب المتابعة")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts) { alert ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("رقم الطالب: ${alert.studentId} - الحالة: ${alert.status}", fontWeight = FontWeight.Bold)
                            Text("التاريخ: ${alert.date}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("توثيق العذر:", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("اتصال", "واتساب", "SMS", "ورقة عذر").forEach { reason ->
                                    OutlinedButton(onClick = {
                                        viewModel.resolveAlert(alert.id, reason, "تم التواصل مع ولي الأمر")
                                        Toast.makeText(context, "تم توثيق الحالة عبر $reason", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text(reason, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة طالب جديد") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("الحلقة / الشعبة") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("هاتف ولي الأمر") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && section.isNotBlank()) {
                    onConfirm(name, section, phone)
                }
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
