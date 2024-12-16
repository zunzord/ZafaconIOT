package com.example.zafaconapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.*
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settingsFragmentContainer, SettingsFragment())
            .commit()

        val btnSaveSettings: Button = findViewById(R.id.btnSaveSettings)
        btnSaveSettings.setOnClickListener {
            guardarCambios()
        }
    }

    private fun guardarCambios() {
        val fragment = supportFragmentManager.findFragmentById(R.id.settingsFragmentContainer) as? SettingsFragment
        fragment?.guardarConfiguraciones(this)

        // Leer las preferencias actuales
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val consultaIntervaloStr = prefs.getString("consulta_intervalo", "15") ?: "15"
        val consultaUnidad = prefs.getString("consulta_unidad", "minutos") ?: "minutos"

        // Convertir el intervalo según la unidad elegida a minutos
        val intervalo = consultaIntervaloStr.toIntOrNull() ?: 15
        val intervaloEnMinutos = when (consultaUnidad) {
            "minutos" -> intervalo
            "horas" -> intervalo * 60
            else -> 15 // valor por defecto en caso de error
        }

        // Reprogramar el Worker con el nuevo intervalo
        programarConsultaWorker(intervaloEnMinutos)

        finish()
    }

    private fun programarConsultaWorker(intervaloMinutos: Int) {
        val workManager = WorkManager.getInstance(this)

        // Constraints: requerir conexión a Internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ConsultaWorker>(
            intervaloMinutos.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Usamos enqueueUniquePeriodicWork para que, si ya existe un trabajo previo, se reemplace
        workManager.enqueueUniquePeriodicWork(
            "CONSULTA_WORKER",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
