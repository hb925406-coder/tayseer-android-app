package com.tayseer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.tayseer.app.data.AttendanceStatus
import com.tayseer.app.data.Student

@Composable
fun FollowUpScreen(
    pendingRecords: List<AttendanceRecord>,
    studentsMap: Map<Long, Student>,
    onResolveFollowUp: (recordId: Long, method: String, note: String) -> Unit
) {
    var selectedRecordForResolve by remember { mutableStateOf<AttendanceRecord?>(null) }
    var followUpMethod by remember { mutableStateOf("اتصال هاتف") }
    var followUpNote by remember { mutableStateOf("") }

    val followUpMethods = listOf("اتصال هاتف", "رسالة واتساب", "رسالة SMS", "ورقة من ولي الأمر")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "التنبيهات والمتابعة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "حالات الغياب والتأخير غير الموثقة: ${pendingRecords.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (pendingRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "رائع! لا توجد حالات غياب أو تأخير معلقة للمتابعة.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(pendingRecords) { record ->
                    val student = studentsMap[record.studentId]
                    val studentName = student?.name ?: "طالب غير معروف"

                    // تحويل الحالة الإنجليزية إلى التسمية العربية الرسمية لتطبيق تيسير مع لونها المناسب
                    val statusText = when (record.status) {
                        "ABSENT", "غائب" -> "غائب"
                        "LATE", "متأخر" -> "متأخر"
                        "TRUANT", "هارب" -> "هارب"
                        "EXCUSED", "مستأذن" -> "مستأذن"
                        "ABSENT_WITH_EXCUSE", "غائب بعذر" -> "غائب بعذر"
                        else -> record.status
                    }

                    val badgeColor = when (record.status) {
                        "ABSENT", "غائب" -> Color(0xFFE57373)
                        "LATE", "متأخر" -> Color(0xFFFFB74D)
                        "TRUANT", "هارب" -> Color(0xFFBA68C8)
                        else -> MaterialTheme.colorScheme.error
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = studentName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "التاريخ: ${record.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (!record.reasonNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "السبب المبدئي: ${record.reasonNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    selectedRecordForResolve = record
                                    followUpMethod = "اتصال هاتف"
                                    followUpNote = ""
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

    selectedRecordForResolve?.let { record ->
        val student = studentsMap[record.studentId]
        val studentName = student?.name ?: "الطالب"

        AlertDialog(
            onDismissRequest = { selectedRecordForResolve = null },
            title = { Text("توثيق عذر: $studentName") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "طريقة التواصل والتبرير:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    followUpMethods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (followUpMethod == method),
                                onClick = { followUpMethod = method }
                            )
                            Text(text = method)
                        }
                    }

                    OutlinedTextField(
                        value = followUpNote,
                        onValueChange = { followUpNote = it },
                        label = { Text("تفاصيل العذر أو الملاحظة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveFollowUp(record.id, followUpMethod, followUpNote.trim())
                        selectedRecordForResolve = null
                    }
                ) {
                    Text("حفظ التوثيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecordForResolve = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
