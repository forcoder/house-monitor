package com.housemonitor.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.housemonitor.OtaEntryPoint
import com.housemonitor.ui.history.HistoryActivity
import com.housemonitor.ui.settings.SettingsScreen
import com.housemonitor.ui.theme.HouseMonitorTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HouseMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 每日自动检查更新
        val prefs = getSharedPreferences("ota_auto_check", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastAutoCheckDay = prefs.getString("last_auto_check_day", "")

        if (lastAutoCheckDay != today) {
            prefs.edit().putString("last_auto_check_day", today).apply()
            // 延迟2秒后检查，避免影响启动体验
            lifecycleScope.launch {
                delay(2000)
                try {
                    val otaManager = EntryPointAccessors.fromApplication(
                        applicationContext,
                        OtaEntryPoint::class.java
                    ).otaManager()
                    otaManager.checkForUpdate()
                } catch (e: Exception) {
                    // 静默失败，不影响APP启动
                }
            }
        }

        // 处理通知点击
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: android.content.Intent) {
        val notificationType = intent.getStringExtra("notification_type")

        when (notificationType) {
            "unavailable_dates" -> {
                val propertyName = intent.getStringExtra("property_name")
                // 导航到历史记录页面，预筛选该房源
                val historyIntent = Intent(this, HistoryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("notification_property", propertyName)
                }
                startActivity(historyIntent)
            }
            "status_change" -> {
                val propertyName = intent.getStringExtra("property_name")
                // 同样导航到历史记录页面查看状态变化
                val historyIntent = Intent(this, HistoryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("notification_property", propertyName)
                }
                startActivity(historyIntent)
            }
        }
    }
}