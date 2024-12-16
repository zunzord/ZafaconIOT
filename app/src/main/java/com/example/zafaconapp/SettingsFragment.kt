package com.example.zafaconapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        // Aquí podrías realizar validaciones adicionales si fueran necesarias
    }

    fun guardarConfiguraciones(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val consultaIntervalo = sharedPreferences.getString("consulta_intervalo", "15") ?: "15"
        val consultaUnidad = sharedPreferences.getString("consulta_unidad", "minutos") ?: "minutos"
        val nivelNotificacion = sharedPreferences.getString("nivel_notificacion", "80") ?: "80"

        Toast.makeText(
            context,
            context.getString(
                R.string.settings_saved_message,
                consultaIntervalo,
                consultaUnidad,
                nivelNotificacion
            ),
            Toast.LENGTH_SHORT
        ).show()

        // No se requiere hacer nada más aquí, la reprogramación del Worker se realiza en SettingsActivity.
    }
}
