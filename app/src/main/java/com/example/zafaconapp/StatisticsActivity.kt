package com.example.zafaconapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var tvFechaNotificacionReciente: TextView
    private lateinit var tvTiempoPromedioVaciado: TextView
    private lateinit var tvEficiencia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        lineChart = findViewById(R.id.lineChart)
        tvFechaNotificacionReciente = findViewById(R.id.tvFechaNotificacionReciente)
        tvTiempoPromedioVaciado = findViewById(R.id.tvTiempoPromedioVaciado)
        tvEficiencia = findViewById(R.id.tvEficiencia)

        configurarGrafico()
        cargarEstadisticas()
    }

    private fun configurarGrafico() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
    }

    private fun cargarEstadisticas() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val eventsJson = prefs.getString("events_list", "[]")
        val jsonArray = JSONArray(eventsJson)

        val events = mutableListOf<Pair<Long, Long>>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val notifiedAt = obj.getLong("notifiedAt")
            val emptiedAt = obj.getLong("emptiedAt")
            if (emptiedAt != -1L) {
                events.add(notifiedAt to emptiedAt)
            }
        }

        if (events.isNotEmpty()) {
            events.sortBy { it.first }

            val lastEvent = events.last()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val fechaStr = sdf.format(Date(lastEvent.first))
            tvFechaNotificacionReciente.text = getString(R.string.stats_last_notification, fechaStr)

            val diferencias = events.map { (notifiedAt, emptiedAt) ->
                (emptiedAt - notifiedAt) / 60000.0 // minutos
            }

            val promedio = diferencias.average()
            tvTiempoPromedioVaciado.text = getString(R.string.stats_avg_time, promedio)

            // Eficiencia: inversamente proporcional al tiempo promedio
            val eficiencia = if (promedio > 0) {
                ((60.0 / promedio) * 100.0).coerceAtMost(100.0)
            } else {
                100.0
            }
            tvEficiencia.text = getString(R.string.stats_efficiency, eficiencia.toInt())

            val entries = diferencias.mapIndexed { index, value ->
                Entry((index + 1).toFloat(), value.toFloat())
            }

            val dataSet = LineDataSet(entries, "Tiempo vaciado (min)").apply {
                color = Color.BLUE
                setCircleColor(Color.BLUE)
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
            }

            lineChart.data = LineData(dataSet)
            lineChart.invalidate()

        } else {
            tvFechaNotificacionReciente.text = getString(R.string.stats_last_notification_none)
            tvTiempoPromedioVaciado.text = getString(R.string.stats_avg_time_none)
            tvEficiencia.text = getString(R.string.stats_efficiency_none)
            lineChart.data = null
            lineChart.invalidate()
        }
    }
}
