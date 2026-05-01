package com.housemonitor.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.housemonitor.ui.history.HistoryActivity
import com.housemonitor.ui.theme.HouseMonitorTheme
import dagger.hilt.android.AndroidEntryPoint

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
                    MainScreen(viewModel = viewModel)
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