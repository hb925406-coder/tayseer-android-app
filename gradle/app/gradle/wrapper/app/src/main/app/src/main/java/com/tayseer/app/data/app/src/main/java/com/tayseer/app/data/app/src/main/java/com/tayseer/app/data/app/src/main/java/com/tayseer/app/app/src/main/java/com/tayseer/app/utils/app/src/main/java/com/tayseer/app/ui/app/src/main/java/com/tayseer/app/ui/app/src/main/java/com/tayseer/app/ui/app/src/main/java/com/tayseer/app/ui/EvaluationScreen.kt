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
import com.tayseer.app.data.EvaluationRecord
import com.tayseer.app.data.Student
import com.tayseer.app.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    dao: AppDao,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val todayHijri = remember { DateUtils.getTodayHijri() }
    val students by dao.getActiveStudents().collectAsState(initial = emptyList())
    var selectedStudentForEval by remember { mutableStateOf<Student?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقييم الطلاب والسلوك",
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
            if (showSuccessMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "تم رصد التقييم وحفظه بنجاح ✓",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students) { student ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = student.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Button(
                                    onClick = { selectedStudentForEval = student },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تقييم الطالب")
                                }
                            }
                        }
                    }
                }
            }

            // نافذة اختيار التقييم السريع
            selectedStudentForEval?.let { student ->
                var selectedCategory by remember { mutableStateOf("MEMORIZED") }
                var customCategoryName by remember { mutableStateOf("") }
                var notes by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { selectedStudentForEval = null },
                    title = { Text("تقييم: ${student.name}") },
                    text = {
                        Column {
                            Text("اختر التقييم:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            val categories = listOf(
                                "MEMORIZED" to "حفظه",
                                "NOT_MEMORIZED" to "لم يحفظ",
                                "EXCELLENT" to "مميز",
                                "POLITE" to "مؤدب",
                                "FORGOT_BOOKS" to "نسيان الكتب",
                                "CUSTOM" to "تقييم مخصص..."
                            )

                            categories.forEach { (catKey, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = (selectedCategory == catKey),
                                        onClick = { selectedCategory = catKey }
                                    )
                                    Text(text = label, modifier = Modifier.padding(start = 8.dp))
                                }
                            }

                            if (selectedCategory == "CUSTOM") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customCategoryName,
                                    onValueChange = { customCategoryName = it },
                                    label = { Text("اكتب التقييم المخصص") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات إضافية (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.insertEvaluation(
                                        EvaluationRecord(
                                            studentId = student.id,
                                            dateHijri = todayHijri,
                                            category = selectedCategory,
                                            customCategoryName = customCategoryName,
                                            notes = notes
                                        )
                                    )
                                    selectedStudentForEval = null
                                    showSuccessMessage = true
                                }
                            }
                        ) {
                            Text("حفظ التقييم")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedStudentForEval = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
