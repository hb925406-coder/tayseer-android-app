package com.tayseer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayseer.app.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    currentUser: User,
    onNavigateToAttendance: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToEvaluations: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة تحكم تيسير",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "المستخدم: ${currentUser.username} (${if (currentUser.role == "ADMIN") "إدارة" else "معلم"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "تسجيل خروج",
                            tint = MaterialTheme.colorScheme.error
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
                .padding(16.dp)
        ) {
            Text(
                text = "الخدمات والعمليات اليومية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    DashboardCard(
                        title = "تسجيل الحضور",
                        subtitle = "رصد الحالات الست اليومية",
                        icon = Icons.Default.DateRange,
                        badgeColor = Color(0xFF2E7D32),
                        onClick = onNavigateToAttendance
                    )
                }

                item {
                    DashboardCard(
                        title = "متابعة الأعذار",
                        subtitle = "تنبيهات الغياب والتأخير",
                        icon = Icons.Default.Notifications,
                        badgeColor = Color(0xFFD32F2F),
                        onClick = onNavigateToAlerts
                    )
                }

                item {
                    DashboardCard(
                        title = "التقييم والسلوك",
                        subtitle = "حفظ القرآن والسلوكيات",
                        icon = Icons.Default.Star,
                        badgeColor = Color(0xFFF57C00),
                        onClick = onNavigateToEvaluations
                    )
                }

                item {
                    DashboardCard(
                        title = "شؤون الطلاب",
                        subtitle = "إضافة وبحث وسجلات",
                        icon = Icons.Default.Person,
                        badgeColor = Color(0xFF1976D2),
                        onClick = onNavigateToStudents
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
