package com.tayseer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tayseer.app.data.User
import com.tayseer.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as TayseerApp
        val dao = app.repository

        setContent {
            MaterialTheme {
                // تفعيل اتجاه الواجهة من اليمين إلى اليسار (RTL) للغة العربية
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var currentUser by remember { mutableStateOf<User?>(null) }
                        var currentScreen by remember { mutableStateOf("LOGIN") }

                        when {
                            currentUser == null -> {
                                LoginScreen(
                                    dao = dao,
                                    onLoginSuccess = { user ->
                                        currentUser = user
                                        currentScreen = if (user.role == "PARENT") "PARENT" else "DASHBOARD"
                                    }
                                )
                            }
                            currentUser?.role == "PARENT" -> {
                                ParentScreen(
                                    dao = dao,
                                    currentUser = currentUser!!,
                                    onLogout = {
                                        currentUser = null
                                        currentScreen = "LOGIN"
                                    }
                                )
                            }
                            else -> {
                                when (currentScreen) {
                                    "DASHBOARD" -> {
                                        MainDashboardScreen(
                                            currentUser = currentUser!!,
                                            onNavigateToAttendance = { currentScreen = "ATTENDANCE" },
                                            onNavigateToAlerts = { currentScreen = "ALERTS" },
                                            onNavigateToEvaluations = { currentScreen = "EVALUATIONS" },
                                            onNavigateToStudents = { currentScreen = "STUDENTS" },
                                            onLogout = {
                                                currentUser = null
                                                currentScreen = "LOGIN"
                                            }
                                        )
                                    }
                                    "ATTENDANCE" -> {
                                        AttendanceScreen(
                                            dao = dao,
                                            onBack = { currentScreen = "DASHBOARD" }
                                        )
                                    }
                                    "ALERTS" -> {
                                        AlertsScreen(
                                            dao = dao,
                                            onBack = { currentScreen = "DASHBOARD" }
                                        )
                                    }
                                    "EVALUATIONS" -> {
                                        EvaluationScreen(
                                            dao = dao,
                                            onBack = { currentScreen = "DASHBOARD" }
                                        )
                                    }
                                    "STUDENTS" -> {
                                        StudentsScreen(
                                            dao = dao,
                                            onBack = { currentScreen = "DASHBOARD" }
                                        )
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
