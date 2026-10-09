package com.jnjl.smartriego

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvHumedad: TextView
    private lateinit var pbHumedad: ProgressBar
    private lateinit var tvBomba: TextView
    private lateinit var btnRegar: Button
    private lateinit var chartHumedad: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvHumedad = findViewById(R.id.tvHumedad)
        pbHumedad = findViewById(R.id.pbHumedad)
        tvBomba = findViewById(R.id.tvBomba)
        btnRegar = findViewById(R.id.btnRegar)
        chartHumedad = findViewById(R.id.chartHumedad)

        configurarGrafico()

        btnRegar.setOnClickListener {
            regarAhora()
        }

        // Android 17 exige permiso para acceder a la red local
        val permisoRed = "android.permission.ACCESS_LOCAL_NETWORK"
        if (ContextCompat.checkSelfPermission(this, permisoRed) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permisoRed), 100)
        }

        // Pide la lectura y el historial cada 5 segundos mientras la app está visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    actualizarTodo()
                    delay(5000)
                }
            }
        }
    }

    private fun configurarGrafico() {
        chartHumedad.description.isEnabled = false
        chartHumedad.axisRight.isEnabled = false
        chartHumedad.axisLeft.axisMinimum = 0f
        chartHumedad.axisLeft.axisMaximum = 100f
        chartHumedad.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartHumedad.xAxis.setLabelCount(4, false)
        chartHumedad.xAxis.setDrawGridLines(false)
        chartHumedad.setNoDataText("Esperando datos...")
    }

    private suspend fun actualizarTodo() {
        try {
            val lectura = RetrofitClient.api.obtenerUltimaLectura()
            mostrarLectura(lectura)
            val registros = RetrofitClient.api.obtenerHistorial()
            mostrarGrafico(registros)
        } catch (e: Exception) {
            tvBomba.text = "Error: ${e.message}"
        }
    }

    private fun regarAhora() {
        btnRegar.isEnabled = false
        lifecycleScope.launch {
            try {
                val lectura = RetrofitClient.api.regarManual()
                mostrarLectura(lectura)
                mostrarGrafico(RetrofitClient.api.obtenerHistorial())
                Toast.makeText(this@MainActivity, "Riego enviado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se pudo enviar el riego", Toast.LENGTH_SHORT).show()
            }
            btnRegar.isEnabled = true
        }
    }

    private fun mostrarLectura(lectura: Lectura) {
        tvHumedad.text = "${lectura.humedad} %"
        pbHumedad.progress = lectura.humedad
        tvBomba.text = if (lectura.bomba_encendida) "Bomba: ENCENDIDA" else "Bomba: APAGADA"
    }

    private fun mostrarGrafico(registros: List<Registro>) {
        val puntos = registros.mapIndexed { i, r -> Entry(i.toFloat(), r.humedad.toFloat()) }

        val dataSet = LineDataSet(puntos, "Humedad (%)").apply {
            color = Color.parseColor("#6750A4")
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#6750A4")
            fillAlpha = 40
        }

        // Muestra la hora de cada lectura en el eje de abajo
        chartHumedad.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return registros.getOrNull(value.toInt())?.hora ?: ""
            }
        }

        chartHumedad.data = LineData(dataSet)
        chartHumedad.invalidate()
    }
}