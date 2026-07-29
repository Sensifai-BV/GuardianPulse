package com.guardianpulse.prototype

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramNotifier {
    private val client = OkHttpClient.Builder()
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
        
    private val token = BuildConfig.TELEGRAM_BOT_TOKEN
    private val chatId1 = BuildConfig.TELEGRAM_CHAT_ID
    private val chatId2 = BuildConfig.TELEGRAM_CHAT_ID_2
    private val chatId3 = BuildConfig.TELEGRAM_CHAT_ID_3
    
    var lastUpdateId = 0L

    suspend fun sendAlert(level: Int) {
        if (token.isBlank()) return
        
        val targetChat = when(level) {
            1 -> chatId1
            2 -> chatId2.ifBlank { chatId1 }
            3 -> chatId3.ifBlank { chatId1 }
            else -> chatId1
        }
        if (targetChat.isBlank()) return

        val text = when(level) {
            1 -> "🚨 GUARDIAN PULSE: LEVEL 1 🚨\n\nPossible distress detected (Heart Rate + Loud Sound). Please check immediately."
            2 -> "⚠️ ESCALATION LEVEL 2 ⚠️\n\nNo response to initial alert. Secondary contact requested to intervene."
            else -> "🚓 ESCALATION LEVEL 3 🚓\n\nPO Escalation: Emergency assistance required."
        }
        
        val url = "https://api.telegram.org/bot$token/sendMessage"
        
        val json = JSONObject().apply {
            put("chat_id", targetChat)
            put("text", text)
            
            // Add inline keyboard for ACK
            val keyboard = JSONArray().apply {
                put(JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "✅ I've checked, all good")
                        put("callback_data", "ack")
                    })
                })
            }
            put("reply_markup", JSONObject().apply {
                put("inline_keyboard", keyboard)
            })
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
            
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("TelegramNotifier", "Level $level Alert sent successfully")
                }
            } catch (e: Exception) {
                Log.e("TelegramNotifier", "Failed sending alert", e)
            }
            Unit
        }
    }
    
    suspend fun sendTamperAlert() {
        if (token.isBlank() || chatId1.isBlank()) return
        
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val json = JSONObject().apply {
            put("chat_id", chatId1)
            put("text", "⚠️ TAMPER ALERT ⚠️\n\nThe Guardian Pulse device has been removed from the user.")
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        
        withContext(Dispatchers.IO) {
            try { client.newCall(request).execute() } catch (e: Exception) {
                Log.e("TelegramNotifier", "Failed sending tamper alert", e)
            }
            Unit
        }
    }
    
    suspend fun sendLowBatteryAlert(level: Int) {
        if (token.isBlank() || chatId1.isBlank()) return
        
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val json = JSONObject().apply {
            put("chat_id", chatId1)
            put("text", "🔋 LOW BATTERY 🔋\n\nThe Guardian Pulse device battery is critically low ($level%). Please recharge immediately.")
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        
        withContext(Dispatchers.IO) {
            try { client.newCall(request).execute() } catch (e: Exception) {
                Log.e("TelegramNotifier", "Failed sending low battery alert", e)
            }
            Unit
        }
    }
    
    suspend fun pollForAck(onAckReceived: () -> Unit) {
        if (token.isBlank()) return
        
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.telegram.org/bot$token/getUpdates?offset=${lastUpdateId + 1}&timeout=30"
                val request = Request.Builder().url(url).get().build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext
                    val json = JSONObject(bodyStr)
                    val result = json.optJSONArray("result") ?: return@withContext
                    
                    for (i in 0 until result.length()) {
                        val update = result.getJSONObject(i)
                        val updateId = update.getLong("update_id")
                        lastUpdateId = maxOf(lastUpdateId, updateId)
                        
                        val callbackQuery = update.optJSONObject("callback_query")
                        if (callbackQuery != null) {
                            val data = callbackQuery.optString("data")
                            if (data == "ack") {
                                onAckReceived()
                                
                                val callbackId = callbackQuery.getString("id")
                                answerCallbackQuery(callbackId, "Alert acknowledged")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramNotifier", "Poll error", e)
                delay(2000)
            }
            Unit
        }
    }
    
    private fun answerCallbackQuery(callbackQueryId: String, text: String) {
        val url = "https://api.telegram.org/bot$token/answerCallbackQuery"
        val json = JSONObject().apply {
            put("callback_query_id", callbackQueryId)
            put("text", text)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
        })
    }
}
