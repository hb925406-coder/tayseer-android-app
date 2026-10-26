package com.tayseer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.AppDao
import com.tayseer.app.data.Student
import com.tayseer.app.data.User
import com.tayseer.app.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(
    dao: AppDao,
    currentUser: User,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var linkedStudent by remember { mutableStateOf<Student?>(null) }

    LaunchedEffect(currentUser) {
        val studentId = currentUser.linkedStudentId ?: 1L
        linkedStudent = dao.getStudentById(studentId)
    }

    val student = linkedStudent
    val attendanceRecords by (if (student != null) dao.getAttendanceForStudent(student.id) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val evaluationRecords by (if (student != null) dao.getEvaluationsForStudent(student.id) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بوابة ولي الأمر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (student == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // بطاقة بيانات الطالب والملخص
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ملف الطالب: ${student.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val absentCount = attendanceRecords.count { it.status == "ABSENT" || it.status == "ABSENT_WITH_EXCUSE" }
                        val lateCount = attendanceRecords.count { it.status == "LATE" }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = "إجمالي الغياب: $absentCount",
                                fontWeight = FontWeight.Bold,
                                color = if (absentCount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = "مرات التأخير: $lateCount",
                                fontWeight = FontWeight.Bold,
                                color = if (lateCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "آخر سجلات الحضور والتقييم:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(attendanceRecords.take(10)) { att ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "التاريخ: ${att.dateHijri} هـ",
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (att.excuseType.isNotBlank()) {
                                        Text(
                                            text = "العذر: ${att.excuseType}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1565C0)
                                        )
                                    }
                                }
                                Text(
                                    text = DateUtils.getStatusLabel(att.status),
                                    fontWeight = FontWeight.Bold,
                                    color = when (att.status) {
                                        "PRESENT" -> Color(0xFF2E7D32)
                                        "ABSENT" -> Color(0xFFC62828)
                                        else -> Color(0xFFE65100)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
