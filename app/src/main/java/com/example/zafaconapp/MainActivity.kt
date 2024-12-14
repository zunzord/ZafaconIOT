package com.example.zafaconapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.Intent

class MainActivity : ComponentActivity() {

    companion object {
        private const val CHANNEL_ID = "zafacon_channel" // Identificador único del canal
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1 // Código de solicitud de permiso
    }

    private lateinit var tvStatus: TextView
    private lateinit var btnObtenerPorcentaje: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Crear el canal de notificaciones al inicio
        crearCanalNotificaciones()

        // Solicitar permiso de notificaciones al abrir la app
        solicitarPermisoNotificaciones()

        // Inicializar botones y vistas
        tvStatus = findViewById(R.id.tvStatus)
        btnObtenerPorcentaje = findViewById(R.id.btnObtenerPorcentaje)

        btnObtenerPorcentaje.setOnClickListener {
            obtenerPorcentaje()
        }

        val btnConfig = findViewById<Button>(R.id.btnConfig)
        btnConfig.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun obtenerPorcentaje() {
        runOnUiThread {
            tvStatus.text = "Conectando..."
        }

        // Leer la IP desde SharedPreferences
        val prefs = getSharedPreferences("com.example.zafacon_app_preferences", Context.MODE_PRIVATE)
        val ip = prefs.getString("arduino_ip", "http://192.168.68.113")!!

        Thread {
            try {
                val url = "$ip/getPercentage"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val porcentaje = json.optDouble("porcentajeLlenado", 0.0)

                    runOnUiThread {
                        tvStatus.text = "Conectado. Porcentaje: $porcentaje%"

                        if (porcentaje >= 80) {
                            mostrarNotificacion()
                        }
                    }
                } else {
                    runOnUiThread {
                        tvStatus.text = "Error en la conexión"
                        Toast.makeText(this, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Error: No se pudo conectar"
                    Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones Importantes"
            val descriptionText = "Canal para notificaciones de nivel alto."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun mostrarNotificacion() {
        // Verifica nuevamente el permiso antes de mostrar la notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Permiso de notificaciones no otorgado.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Zafacón Inteligente")
            .setContentText("El nivel de llenado ha alcanzado el 80% o más.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permiso de notificaciones otorgado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de notificaciones denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
