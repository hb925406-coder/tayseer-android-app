package com.tayseer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.tayseer.app.data.Student
import com.tayseer.app.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    dao: AppDao,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val todayHijri = remember { DateUtils.getTodayHijri() }
    val todayGregorian = remember { DateUtils.getTodayGregorian() }

    val students by dao.getActiveStudents().collectAsState(initial = emptyList())
    val attendanceList by dao.getAttendanceByDate(todayHijri).collectAsState(initial = emptyList())

    // خريطة لتخزين حالة كل طالب مؤقتاً أثناء التحديد
    val attendanceMap = remember(attendanceList) {
        attendanceList.associateBy { it.studentId }.toMutableMap()
    }

    var showSaveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تسجيل الحضور اليومي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "التاريخ: $todayHijri هـ ($todayGregorian م)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val recordsToSave = students.map { student ->
                                    val current = attendanceMap[student.id]
                                    AttendanceRecord(
                                        id = current?.id ?: 0,
                                        studentId = student.id,
                                        dateHijri = todayHijri,
                                        dateGregorian = todayGregorian,
                                        status = current?.status ?: "PRESENT",
                                        followUpStatus = if (current?.status in listOf("ABSENT", "LATE", "ESCAPED")) "PENDING" else "NONE"
                                    )
                                }
                                dao.insertAttendanceBatch(recordsToSave)
                                showSaveSuccess = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "حفظ السجل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (showSaveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "تم حفظ كشف الحضور بنجاح!",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد طلاب مضافون حالياً",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(students) { student ->
                        val currentRecord = attendanceMap[student.id]
                        val selectedStatus = currentRecord?.status ?: "PRESENT"

                        StudentAttendanceItem(
                            student = student,
                            selectedStatus = selectedStatus,
                            onStatusSelected = { newStatus ->
                                val updatedRecord = (attendanceMap[student.id] ?: AttendanceRecord(
                                    studentId = student.id,
                                    dateHijri = todayHijri,
                                    dateGregorian = todayGregorian,
                                    status = newStatus
                                )).copy(status = newStatus)
                                attendanceMap[student.id] = updatedRecord
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceItem(
    student: Student,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = student.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // الحالات الست بأزرار سريعة ومميزة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusChip("حاضر", "PRESENT", selectedStatus, Color(0xFF4CAF50), Modifier.weight(1f)) { onStatusSelected(it) }
                StatusChip("غائب", "ABSENT", selectedStatus, Color(0xFFE53935), Modifier.weight(1f)) { onStatusSelected(it) }
                StatusChip("مستأذن", "EXCUSED", selectedStatus, Color(0xFFFB8C00), Modifier.weight(1f)) { onStatusSelected(it) }
                StatusChip("متأخر", "LATE", selectedStatus, Color(0xFFFFB300), Modifier.weight(1f)) { onStatusSelected(it) }
                StatusChip("عذر", "ABSENT_WITH_EXCUSE", selectedStatus, Color(0xFF039BE5), Modifier.weight(1f)) { onStatusSelected(it) }
                StatusChip("هارب", "ESCAPED", selectedStatus, Color(0xFF8E24AA), Modifier.weight(1f)) { onStatusSelected(it) }
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    statusCode: String,
    currentSelected: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val isSelected = currentSelected == statusCode
    Button(
        onClick = { onSelect(statusCode) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.white else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(34.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
