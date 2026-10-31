package com.tayseer.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tayseer.app.data.AppDatabase
import com.tayseer.app.data.CustomEvaluation
import com.tayseer.app.data.Student
import com.tayseer.app.ui.MainViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(database.appDao()) as T
            }
        }

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainViewModel: MainViewModel = viewModel(factory = factory)
                    TayseerApp(viewModel = mainViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayseerApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تطبيق تيسير", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("تسجيل الدخول", fontSize = 16.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = loginError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.login(usernameInput, passwordInput) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("دخول", fontSize = 16.sp)
                    }
                }
            }
        }
    } else {
        MainDashboard(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val displayDate by viewModel.displayDate.collectAsState()
    val isHijri by viewModel.isHijri.collectAsState()
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()
    val customEvaluations by viewModel.customEvaluations.collectAsState()

    var currentScreen by remember { mutableStateOf("home") }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    var selectedStudentForEval by remember { mutableStateOf<Student?>(null) }
    var showAddCustomEvalDialog by remember { mutableStateOf(false) }
    var newCustomEvalTitle by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importStudentsFromCsv(context, uri, viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentUser?.role) {
                                "ADMIN" -> "تيسير - لوحة الإدارة"
                                "TEACHER" -> "تيسير - حساب المعلم"
                                else -> "تيسير - متابعة ولي الأمر"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.clickable { viewModel.toggleCalendarType() }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleCalendarType() }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "تبديل التقويم"
                        )
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "تسجيل خروج"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                if (currentUser?.role != "PARENT") {
                    NavigationBarItem(
                        selected = currentScreen == "alerts",
                        onClick = { currentScreen = "alerts" },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingAlerts.isNotEmpty()) {
                                        Badge { Text(pendingAlerts.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                            }
                        },
                        label = { Text("التنبيهات") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == "home" && currentUser?.role == "ADMIN") {
                FloatingActionButton(onClick = { showAddStudentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (currentScreen == "home") {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (currentUser?.role == "ADMIN") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("استيراد بيانات الطلاب (CSV):", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { csvLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "application/csv")) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختيار ملف", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (currentUser?.role != "PARENT") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة الطلاب (${students.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.markAllPresent(students) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تحضير الكل", fontSize = 12.sp)
                            }
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(students) { student ->
                            StudentCard(
                                student = student,
                                canEdit = currentUser?.role == "ADMIN",
                                onAttendance = { status ->
                                    viewModel.markAttendance(student.id, status)
                                    Toast.makeText(context, "تم تسجيل: $status للطالب ${student.name}", Toast.LENGTH_SHORT).show()
                                },
                                onEvaluate = {
                                    selectedStudentForEval = student
                                },
                                onEdit = {
                                    studentToEdit = student
                                },
                                onDelete = {
                                    studentToDelete = student
                                }
                            )
                        }
                    }
                }
            } else if (currentScreen == "alerts") {
                AlertsScreen(viewModel = viewModel)
            }
        }
    }

    if (showAddStudentDialog) {
        StudentFormDialog(
            title = "إضافة طالب جديد",
            student = null,
            onDismiss = { showAddStudentDialog = false },
            onConfirm = { name, section, phone ->
                viewModel.insertStudent(Student(name = name, section = section, parentPhone = phone))
                showAddStudentDialog = false
                Toast.makeText(context, "تمت إضافة الطالب بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (studentToEdit != null) {
        StudentFormDialog(
            title = "تعديل بيانات الطالب",
            student = studentToEdit,
            onDismiss = { studentToEdit = null },
            onConfirm = { name, section, phone ->
                studentToEdit?.let { current ->
                    viewModel.updateStudent(current.copy(name = name, section = section, parentPhone = phone))
                    Toast.makeText(context, "تم تعديل البيانات بنجاح", Toast.LENGTH_SHORT).show()
                }
                studentToEdit = null
            }
        )
    }

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف الطالب: ${studentToDelete?.name}؟") },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete?.let { target ->
                            viewModel.deleteStudent(target)
                            Toast.makeText(context, "تم حذف الطالب بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (selectedStudentForEval != null) {
        AlertDialog(
            onDismissRequest = { selectedStudentForEval = null },
            title = {
                Text(
                    text = "تقييم الطالب: ${selectedStudentForEval?.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text("التقييمات الأساسية:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    val defaultEvals = listOf("حفظه", "لم يحفظ", "مميز", "مؤدب", "نسيان الكتب")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        defaultEvals.take(3).forEach { eval ->
                            OutlinedButton(
                                onClick = {
                                    selectedStudentForEval?.let { student ->
                                        viewModel.markEvaluation(student.id, eval)
                                        Toast.makeText(context, "تم التقييم: $eval", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedStudentForEval = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(eval, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        defaultEvals.drop(3).forEach { eval ->
                            OutlinedButton(
                                onClick = {
                                    selectedStudentForEval?.let { student ->
                                        viewModel.markEvaluation(student.id, eval)
                                        Toast.makeText(context, "تم التقييم: $eval", Toast.LENGTH_SHORT).show()
                                    }
                                    selectedStudentForEval = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(eval, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تقييمات مخصصة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        IconButton(
                            onClick = {
                                newCustomEvalTitle = ""
                                showAddCustomEvalDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "إضافة تقييم", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (customEvaluations.isEmpty()) {
                        Text("لا توجد تقييمات مخصصة مضافة حالياً.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                            items(customEvaluations) { custom ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = custom.title,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedStudentForEval?.let { student ->
                                                    viewModel.markEvaluation(student.id, custom.title)
                                                    Toast.makeText(context, "تم التقييم: ${custom.title}", Toast.LENGTH_SHORT).show()
                                                }
                                                selectedStudentForEval = null
                                            }
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteCustomEvaluation(custom) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedStudentForEval = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showAddCustomEvalDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomEvalDialog = false },
            title = { Text("إضافة تقييم مخصص جديد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCustomEvalTitle,
                    onValueChange = { newCustomEvalTitle = it },
                    label = { Text("عنوان التقييم (مثال: مشارك، متقن، نائم)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomEvalTitle.isNotBlank()) {
                            viewModel.addCustomEvaluation(newCustomEvalTitle)
                            showAddCustomEvalDialog = false
                            Toast.makeText(context, "تمت إضافة التقييم المخصص", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCustomEvalDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    canEdit: Boolean,
    onAttendance: (String) -> Unit,
    onEvaluate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = student.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "الشعبة / الحلقة: ${student.section}", fontSize = 13.sp, color = Color.Gray)
                }
                Row {
                    IconButton(onClick = onEvaluate) {
                        Icon(Icons.Default.Star, contentDescription = "تقييم", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val statuses = listOf("حاضر", "غائب", "مستأذن", "متأخر", "غائب بعذر", "هارب")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(statuses) { status ->
                    FilterChip(
                        selected = false,
                        onClick = { onAttendance(status) },
                        label = { Text(status, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentFormDialog(
    title: String,
    student: Student?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, section: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(student?.name ?: "") }
    var section by remember { mutableStateOf(student?.section ?: "") }
    var phone by remember { mutableStateOf(student?.parentPhone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("الحلقة / الشعبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("هاتف ولي الأمر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && section.isNotBlank()) {
                        onConfirm(name, section, phone)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val pendingAlerts by viewModel.pendingAlerts.collectAsState()

    if (pendingAlerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد حالات غياب أو تأخير تتطلب المتابعة.", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(pendingAlerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "حالة: ${alert.status} - التاريخ: ${alert.date}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val types = listOf("اتصال", "واتساب", "SMS", "ورقة عذر")
                            types.forEach { type ->
                                Button(
                                    onClick = { viewModel.resolveAlert(alert.id, type, "تم التوثيق") },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(type, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun importStudentsFromCsv(context: Context, uri: Uri, viewModel: MainViewModel) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val studentsList = mutableListOf<Student>()
                var line: String? = reader.readLine()
                var isHeader = true
                while (line != null) {
                    if (isHeader) {
                        isHeader = false
                        line = reader.readLine()
                        continue
                    }
                    val tokens = line.split(",")
                    if (tokens.size >= 2) {
                        val name = tokens[0].trim()
                        val section = tokens[1].trim()
                        val phone = if (tokens.size >= 3) tokens[2].trim() else ""
                        if (name.isNotEmpty() && section.isNotEmpty()) {
                            studentsList.add(Student(name = name, section = section, parentPhone = phone))
                        }
                    }
                    line = reader.readLine()
                }
                if (studentsList.isNotEmpty()) {
                    viewModel.insertStudentsList(studentsList)
                    Toast.makeText(context, "تم استيراد ${studentsList.size} طالب بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء قراءة الملف", Toast.LENGTH_SHORT).show()
    }
}
