package com.tayseer.app.ui

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
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    dao: AppDao,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pendingAlerts by dao.getPendingFollowUps().collectAsState(initial = emptyList())
    val students by dao.getActiveStudents().collectAsState(initial = emptyList())
    val studentsMap = remember(students) { students.associateBy { it.id } }

    var selectedRecordForExcuse by remember { mutableStateOf<AttendanceRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تنبيهات الغياب والتأخير",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
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
            if (pendingAlerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حالات غياب أو تأخير معلقة حالياً ✓",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingAlerts) { record ->
                        val student = studentsMap[record.studentId]
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = student?.name ?: "طالب #${record.studentId}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Badge(
                                        containerColor = if (record.status == "ABSENT") Color(0xFFFFCDD2) else Color(0xFFFFF9C4)
                                    ) {
                                        Text(
                                            text = DateUtils.getStatusLabel(record.status),
                                            color = if (record.status == "ABSENT") Color(0xFFC62828) else Color(0xFFF57F17),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "التاريخ: ${record.dateHijri} هـ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (student?.parentPhone?.isNotBlank() == true) {
                                    Text(
                                        text = "هاتف ولي الأمر: ${student.parentPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { selectedRecordForExcuse = record },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("توثيق العذر والمتابعة")
                                }
                            }
                        }
                    }
                }
            }

            // نافذة توثيق العذر واختيار نوعه
            selectedRecordForExcuse?.let { record ->
                var selectedExcuseType by remember { mutableStateOf("ورقة من ولي الأمر") }
                var excuseNote by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { selectedRecordForExcuse = null },
                    title = { Text("توثيق عذر الطالب") },
                    text = {
                        Column {
                            Text("اختر طريقة وصول العذر:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            listOf(
                                "ورقة من ولي الأمر",
                                "اتصال هاتفي",
                                "رسالة واتساب",
                                "رسالة SMS"
                            ).forEach { type ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = (selectedExcuseType == type),
                                        onClick = { selectedExcuseType = type }
                                    )
                                    Text(text = type, modifier = Modifier.padding(start = 8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = excuseNote,
                                onValueChange = { excuseNote = it },
                                label = { Text("ملاحظة إضافية (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.resolveFollowUp(
                                        recordId = record.id,
                                        status = "RESOLVED",
                                        excuseType = selectedExcuseType,
                                        note = excuseNote
                                    )
                                    selectedRecordForExcuse = null
                                }
                            }
                        ) {
                            Text("حفظ وإنهاء التنبيه")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedRecordForExcuse = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
