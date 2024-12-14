package com.example.zafaconapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

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
        // Guardar configuraciones y brindar feedback al usuario
        val fragment = supportFragmentManager.findFragmentById(R.id.settingsFragmentContainer) as? SettingsFragment
        fragment?.guardarConfiguraciones(this)

        // Finalizar y volver a MainActivity
        finish()
    }
}
