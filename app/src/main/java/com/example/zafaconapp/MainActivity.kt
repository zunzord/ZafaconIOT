package com.example.zafaconapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var imgPercentage: ImageView
    private lateinit var btnObtenerPorcentaje: Button
    private lateinit var btnConfig: Button

    private var arduinoIpAddress: String? = null
    private val client = OkHttpClient()
    private val channelId = "ZAFACON_NOTIFICATION_CHANNEL"

    // Para mDNS con NsdManager
    private lateinit var nsdManager: NsdManager
    private var discoveryActive = false
    private val serviceType = "_http._tcp."

    // Especificar tipos explícitos para evitar problemas de inferencia
    private val discoveryListener: NsdManager.DiscoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String?) {
            runOnUiThread {
                tvStatus.text = "Buscando servicios HTTP..."
            }
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            discoveryActive = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let {
                val name: String = it.serviceName
                // Si el servicio encontrado contiene "arduino" en el nombre
                if (name.contains("arduino", ignoreCase = true)) {
                    nsdManager.resolveService(it, resolveListener)
                }
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            runOnUiThread {
                tvStatus.text = "Arduino desconectado"
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
            discoveryActive = false
            runOnUiThread {
                tvStatus.text = "Error al iniciar descubrimiento"
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
            discoveryActive = false
            runOnUiThread {
                tvStatus.text = "Error al detener descubrimiento"
            }
        }
    }

    private val resolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            runOnUiThread {
                tvStatus.text = "No se pudo resolver el servicio"
            }
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let {
                val host = it.host
                val port = it.port
                val ip: String? = host?.hostAddress
                runOnUiThread {
                    if (ip != null) {
                        arduinoIpAddress = "http://$ip:$port"
                        tvStatus.text = "Arduino en: $arduinoIpAddress"
                    } else {
                        tvStatus.text = "No se encontró la IP del Arduino"
                    }
                }
                nsdManager.stopServiceDiscovery(discoveryListener)
                discoveryActive = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        imgPercentage = findViewById(R.id.imgPercentage)
        btnObtenerPorcentaje = findViewById(R.id.btnObtenerPorcentaje)
        btnConfig = findViewById(R.id.btnConfig)

        checkNotificationPermission()
        createNotificationChannel()

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        // Iniciar búsqueda del servicio Arduino
        discoverArduinoService()

        btnObtenerPorcentaje.setOnClickListener {
            obtenerPorcentaje()
        }

        btnConfig.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun discoverArduinoService() {
        if (!discoveryActive) {
            discoveryActive = true
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    private fun obtenerPorcentaje() {
        val ip = arduinoIpAddress ?: run {
            tvStatus.text = "Error: Arduino no detectado"
            return
        }
        val request = Request.Builder().url("$ip/getPercentage").get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    // Debido a que JSONObject a veces genera sobrecargas, especificamos el constructor
                    val json = JSONObject(body)
                    val porcentaje = json.optDouble("porcentajeLlenado", 0.0).toFloat()

                    runOnUiThread {
                        tvStatus.text = "Porcentaje: $porcentaje%"
                        updateImage(porcentaje)
                        if (porcentaje >= 80) {
                            mostrarNotificacion(porcentaje)
                        }
                    }
                } else {
                    runOnUiThread {
                        tvStatus.text = "Error: Respuesta inválida del servidor"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Error: Fallo en la conexión"
                }
            }
        }.start()
    }

    private fun updateImage(porcentaje: Float) {
        val imageResource = when (porcentaje.toInt()) {
            in 0..9 -> R.drawable.level_0
            in 10..19 -> R.drawable.level_10
            in 20..29 -> R.drawable.level_20
            in 30..39 -> R.drawable.level_30
            in 40..49 -> R.drawable.level_40
            in 50..59 -> R.drawable.level_50
            in 60..69 -> R.drawable.level_60
            in 70..79 -> R.drawable.level_70
            in 80..89 -> R.drawable.level_80
            in 90..99 -> R.drawable.level_90
            else -> R.drawable.level_100
        }
        imgPercentage.setImageResource(imageResource)
    }

    private fun mostrarNotificacion(porcentaje: Float) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zafacón lleno")
            .setContentText("El zafacón está lleno al $porcentaje%.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(0, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones Zafacón"
            val descriptionText = "Notificaciones de llenado del zafacón"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}
