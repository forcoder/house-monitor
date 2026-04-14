package com.workbuddy.house.monitor.service

import android.util.Log
import com.google.gson.Gson
import com.workbuddy.house.monitor.model.AvailabilityResult
import com.workbuddy.house.monitor.model.DateAvailability
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MtMeituanMonitor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val gson = Gson()

    fun checkAvailability(meituanUrl: String, callback: (Result<AvailabilityResult>) -> Unit) {
        try {
            // 模拟美团小程序的API调用
            // 实际应用中需要分析美团小程序的具体API接口
            val request = Request.Builder()
                .url(buildApiUrl(meituanUrl))
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; MI 9) AppleWebKit/537.36")
                .addHeader("Referer", meituanUrl)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("MtMeituanMonitor", "API调用失败", e)
                    callback(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            if (responseBody != null) {
                                val availabilityResult = parseAvailabilityData(responseBody)
                                callback(Result.success(availabilityResult))
                            } else {
                                callback(Result.failure(Exception("响应体为空")))
                            }
                        } else {
                            callback(Result.failure(Exception("HTTP错误: ${response.code}")))
                        }
                    } catch (e: Exception) {
                        Log.e("MtMeituanMonitor", "解析响应失败", e)
                        callback(Result.failure(e))
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("MtMeituanMonitor", "创建请求失败", e)
            callback(Result.failure(e))
        }
    }

    private fun buildApiUrl(meituanUrl: String): String {
        // 这里需要根据实际的美团小程序URL构建API请求
        // 这只是一个示例实现
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_MONTH, 30)
        val endDate = dateFormat.format(calendar.time)

        return "${meituanUrl}/api/availability?start_date=$today&end_date=$endDate"
    }

    private fun parseAvailabilityData(responseBody: String): AvailabilityResult {
        return try {
            // 模拟解析美团API响应
            // 实际应用中需要根据美团API的实际响应格式来解析
            val unavailableDates = mutableListOf<DateAvailability>()

            // 模拟数据 - 实际应该从responseBody中解析
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 模拟一些无房的日期
            repeat(5) { i ->
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                if (Random().nextBoolean()) {
                    unavailableDates.add(
                        DateAvailability(
                            date = dateFormat.format(calendar.time),
                            available = false,
                            price = null
                        )
                    )
                }
            }

            AvailabilityResult(
                propertyId = "mt_property_001",
                checkTime = System.currentTimeMillis(),
                unavailableDates = unavailableDates,
                totalDays = 30
            )
        } catch (e: Exception) {
            Log.e("MtMeituanMonitor", "解析房源数据失败", e)
            AvailabilityResult(
                propertyId = "unknown",
                checkTime = System.currentTimeMillis(),
                unavailableDates = emptyList(),
                totalDays = 0
            )
        }
    }
}