package com.example.zafaconapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val consultaIntervalo = findPreference<EditTextPreference>("consulta_intervalo")
        val consultaUnidad = findPreference<Preference>("consulta_unidad")
        val nivelNotificacion = findPreference<EditTextPreference>("nivel_notificacion")

        consultaIntervalo?.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull()
            val unidad = consultaUnidad?.sharedPreferences?.getString("consulta_unidad", "minutos")
            if (value == null || !validarTiempo(value, unidad)) {
                Toast.makeText(context, getString(R.string.invalid_interval), Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        }

        nivelNotificacion?.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toFloatOrNull()
            if (value == null || value !in 10f..100f) {
                Toast.makeText(context, getString(R.string.invalid_notification_level), Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        }
    }

    private fun validarTiempo(value: Int, unidad: String?): Boolean {
        return when (unidad) {
            "minutos" -> value in 15..60 // Restricción de mínimo 15 minutos
            "horas" -> value in 1..24
            else -> false
        }
    }

    fun guardarConfiguraciones(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val consultaIntervalo = sharedPreferences.getString("consulta_intervalo", "15")
        val consultaUnidad = sharedPreferences.getString("consulta_unidad", "minutos")
        val nivelNotificacion = sharedPreferences.getString("nivel_notificacion", "80")

        Toast.makeText(
            context,
            getString(
                R.string.settings_saved_message,
                consultaIntervalo,
                consultaUnidad,
                nivelNotificacion
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}
