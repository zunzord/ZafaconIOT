package com.example.zafaconapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ConsultaWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val client = OkHttpClient()
    private val channelId = "ZAFACON_NOTIFICATION_CHANNEL"

    override fun doWork(): Result {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val arduinoIP = prefs.getString("arduino_ip", "http://192.168.68.113") ?: "http://192.168.68.113"
        val url = "$arduinoIP/getPercentage"

        val request = Request.Builder().url(url).get().build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val porcentaje = json.optDouble("porcentajeLlenado", 0.0).toFloat()

                if (porcentaje >= getNotificationLevel()) {
                    sendNotification(porcentaje)

                    // Registrar un nuevo evento: notifiedAt = ahora, emptiedAt = -1
                    val currentTime = System.currentTimeMillis()
                    addNewEvent(currentTime)
                }

                Result.success()
            } else {
                logError("Respuesta inválida del servidor")
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logError("Fallo en la conexión: ${e.message}")
            Result.retry()
        }
    }

    private fun getNotificationLevel(): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        return prefs.getString("nivel_notificacion", "80")!!.toFloat()
    }

    private fun sendNotification(porcentaje: Float) {
        createNotificationChannel()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.notification_text,
                    porcentaje
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(0, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.channel_name)
            val descriptionText = applicationContext.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logError(message: String) {
        println("ConsultaWorker Error: $message")
    }

    private fun addNewEvent(notifiedAt: Long) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val eventsJson = prefs.getString("events_list", "[]")
        val jsonArray = JSONArray(eventsJson)

        val newEvent = JSONObject()
        newEvent.put("notifiedAt", notifiedAt)
        newEvent.put("emptiedAt", -1)

        jsonArray.put(newEvent)

        prefs.edit().putString("events_list", jsonArray.toString()).apply()
    }
}
