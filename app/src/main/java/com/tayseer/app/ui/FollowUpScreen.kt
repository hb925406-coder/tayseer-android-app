package com.tayseer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.AttendanceRecord
import com.tayseer.app.data.Student
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpScreen(
    pendingRecords: List<AttendanceRecord>,
    studentsMap: Map<Long, Student>,
    onResolveFollowUp: (recordId: Long, method: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var selectedMethod by remember { mutableStateOf("ورقة من ولي الأمر") }
    var noteText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val excuseMethods = listOf(
        "ورقة من ولي الأمر",
        "اتصال هاتف",
        "رسالة واتساب",
        "رسالة SMS"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "التنبيهات والمتابعة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "حالات الغياب والتأخير غير الموثقة: ${pendingRecords.size}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "رائع! لا توجد حالات غياب أو تأخير معلقة للمتابعة.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pendingRecords, key = { it.id }) { record ->
                    val student = studentsMap[record.studentId]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = student?.name ?: "طالب #${record.studentId}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = when (record.status) {
                                        "غائب", "ABSENT" -> Color(0xFFE57373)
                                        "متأخر", "LATE" -> Color(0xFFFFB74D)
                                        else -> Color(0xFFBA68C8)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = record.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "التاريخ: ${record.date}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (student?.parentPhone?.isNotEmpty() == true) {
                                Text(
                                    text = "هاتف ولي الأمر: ${student.parentPhone}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    selectedRecord = record
                                    selectedMethod = "ورقة من ولي الأمر"
                                    noteText = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("توثيق العذر وإغلاق التنبيه")
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRecord != null) {
        val target = selectedRecord!!
        val student = studentsMap[target.studentId]

        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = {
                Text(
                    text = "توثيق عذر: ${student?.name ?: "طالب"}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اختر وسيلة التوثيق:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    excuseMethods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (selectedMethod == method),
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = method, fontSize = 14.sp)
                        }
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("سبب العذر / ملاحظة إضافية") },
                        placeholder = { Text("مثال: مريض بتقرير طبي أو ظرف عائلي") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveFollowUp(target.id, selectedMethod, noteText)
                        selectedRecord = null
                    }
                ) {
                    Text("حفظ وإنهاء المتابعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecord = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
