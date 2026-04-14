package com.workbuddy.house.monitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.workbuddy.house.monitor.databinding.ActivityMainBinding
import com.workbuddy.house.monitor.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            btnStartMonitoring.setOnClickListener {
                val meituanUrl = etMeituanUrl.text.toString().trim()
                if (meituanUrl.isNotEmpty()) {
                    viewModel.startMonitoring(meituanUrl)
                } else {
                    etMeituanUrl.error = "请输入美团小程序地址"
                }
            }

            btnStopMonitoring.setOnClickListener {
                viewModel.stopMonitoring()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.monitoringStatus.observe(this) { isMonitoring ->
            binding.apply {
                btnStartMonitoring.isEnabled = !isMonitoring
                btnStopMonitoring.isEnabled = isMonitoring
                if (isMonitoring) {
                    tvStatus.text = "监控状态: 运行中"
                } else {
                    tvStatus.text = "监控状态: 已停止"
                }
            }
        }

        viewModel.lastCheckTime.observe(this) { time ->
            binding.tvLastCheck.text = "最后检查时间: $time"
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                binding.tvStatus.text = "错误: $it"
            }
        }
    }
}