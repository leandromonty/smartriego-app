package com.jnjl.smartriego

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    private lateinit var tvEquipo: TextView
    private lateinit var tvHumedad: TextView
    private lateinit var pbHumedad: ProgressBar
    private lateinit var tvBomba: TextView
    private lateinit var btnRegar: Button
    private lateinit var btnSalir: Button
    private lateinit var btnEquipo: Button
    private lateinit var chartHumedad: LineChart

    // Equipo que se está mostrando. -1 = todavía no se eligió ninguno
    private var dispositivoId = -1
    private var etiquetaEquipo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvEquipo = findViewById(R.id.tvEquipo)
        tvHumedad = findViewById(R.id.tvHumedad)
        pbHumedad = findViewById(R.id.pbHumedad)
        tvBomba = findViewById(R.id.tvBomba)
        btnRegar = findViewById(R.id.btnRegar)
        btnSalir = findViewById(R.id.btnSalir)
        btnEquipo = findViewById(R.id.btnEquipo)
        chartHumedad = findViewById(R.id.chartHumedad)

        // Recupera el último equipo elegido (si lo hay)
        dispositivoId = RetrofitClient.sesion.obtenerEquipo()

        configurarGrafico()

        btnRegar.setOnClickListener { regarAhora() }
        btnSalir.setOnClickListener { cerrarSesion() }
        btnEquipo.setOnClickListener { elegirEquipo() }

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

    // Guarda cuál es el equipo activo y lo muestra arriba
    private fun seleccionarEquipo(equipo: DispositivoApp) {
        dispositivoId = equipo.id
        etiquetaEquipo = "${equipo.nombre} · ${equipo.codigo_activacion}"
        tvEquipo.text = "Equipo: $etiquetaEquipo"
        RetrofitClient.sesion.guardarEquipo(equipo.id)
    }

    // Si todavía no hay equipo elegido (o falta su nombre), lo busca en el servidor
    private suspend fun asegurarEquipo(): Boolean {
        if (dispositivoId != -1 && etiquetaEquipo.isNotEmpty()) return true

        val equipos = RetrofitClient.api.obtenerDispositivos()
        if (equipos.isEmpty()) {
            tvEquipo.text = "Equipo: --"
            tvBomba.text = "No tenés equipos. Comprá uno en la web."
            return false
        }
        val elegido = equipos.firstOrNull { it.id == dispositivoId } ?: equipos.first()
        seleccionarEquipo(elegido)
        return true
    }

    private suspend fun actualizarTodo() {
        try {
            if (!asegurarEquipo()) return

            val idActual = dispositivoId
            val lectura = RetrofitClient.api.obtenerUltimaLectura(idActual)
            val registros = RetrofitClient.api.obtenerHistorial(idActual)

            // Si cambió de equipo mientras esperaba la respuesta, se descarta
            if (idActual != dispositivoId) return

            mostrarLectura(lectura)
            mostrarGrafico(registros)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> cerrarSesion()
                404 -> tvBomba.text = "El equipo todavía no envió datos"
                else -> tvBomba.text = "Error del servidor (${e.code()})"
            }
        } catch (e: Exception) {
            tvBomba.text = "No se pudo conectar con el servidor"
        }
    }

    private fun elegirEquipo() {
        lifecycleScope.launch {
            try {
                val equipos = RetrofitClient.api.obtenerDispositivos()
                if (equipos.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No tenés equipos", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nombres = equipos
                    .map { "${it.nombre} · ${it.codigo_activacion}" }
                    .toTypedArray()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Elegí un equipo")
                    .setItems(nombres) { _, posicion ->
                        seleccionarEquipo(equipos[posicion])
                        limpiarPantalla()
                        lifecycleScope.launch { actualizarTodo() }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: HttpException) {
                if (e.code() == 401) cerrarSesion()
                else Toast.makeText(this@MainActivity, "No se pudo cargar la lista", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se pudo conectar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun limpiarPantalla() {
        tvHumedad.text = "-- %"
        pbHumedad.progress = 0
        tvBomba.text = "Cargando..."
        chartHumedad.clear()
    }

    private fun regarAhora() {
        if (dispositivoId == -1) {
            Toast.makeText(this, "Todavía no se cargó el equipo", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegar.isEnabled = false
        lifecycleScope.launch {
            try {
                val lectura = RetrofitClient.api.regarManual(dispositivoId)
                mostrarLectura(lectura)
                Toast.makeText(this@MainActivity, "Riego enviado al equipo", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                val mensaje = when (e.code()) {
                    401 -> "Tu sesión venció"
                    409 -> "El equipo no está conectado todavía"
                    else -> "No se pudo enviar el riego"
                }
                Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
                if (e.code() == 401) cerrarSesion()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "No se pudo conectar", Toast.LENGTH_SHORT).show()
            }
            btnRegar.isEnabled = true
        }
    }

    private fun cerrarSesion() {
        RetrofitClient.sesion.cerrarSesion()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarLectura(lectura: Lectura) {
        tvHumedad.text = "${lectura.humedad} %"
        pbHumedad.progress = lectura.humedad

        var texto = if (lectura.bomba_encendida) "Bomba: ENCENDIDA" else "Bomba: APAGADA"
        if (lectura.riego_pendiente) texto += "\nRiego en camino al equipo..."
        if (lectura.deposito_bajo) texto += "\n⚠ Depósito de agua bajo"
        tvBomba.text = texto
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