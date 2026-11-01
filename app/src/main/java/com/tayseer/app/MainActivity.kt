package com.tayseer.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val statusMessage by viewModel.importStatusMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var studentNameInput by remember { mutableStateOf("") }
    var studentSectionInput by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تطبيق تيسير - إدارة الطلاب") },
                actions = {
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
            Text(
                text = "إجمالي الطلاب المسجلين: ${students.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد طلاب مسجلون حالياً. اضغط على أيقونة الملف لاستيراد قائمة الطلاب أو زر + لإضافة طالب.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students) { student ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "الحلقة/الشعبة: ${student.section}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (student.parentPhone.isNotBlank()) {
                                    Text(
                                        text = "هاتف ولي الأمر: ${student.parentPhone}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
