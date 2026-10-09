package com.jnjl.smartriego

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvError: TextView
    private lateinit var btnIngresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión guardada, entra directo a la pantalla principal
        if (RetrofitClient.sesion.obtenerToken() != null) {
            irAPrincipal()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvError = findViewById(R.id.tvError)
        btnIngresar = findViewById(R.id.btnIngresar)

        // Android 17 exige este permiso antes de usar la red local
        val permisoRed = "android.permission.ACCESS_LOCAL_NETWORK"
        if (ContextCompat.checkSelfPermission(this, permisoRed) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permisoRed), 100)
        }

        btnIngresar.setOnClickListener { ingresar() }
    }

    private fun ingresar() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            tvError.text = "Completá email y contraseña."
            return
        }

        tvError.text = ""
        btnIngresar.isEnabled = false

        lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.api.login(LoginRequest(email, password))
                RetrofitClient.sesion.guardarToken(respuesta.access_token)
                irAPrincipal()
            } catch (e: HttpException) {
                tvError.text = if (e.code() == 401) {
                    "Email o contraseña incorrectos."
                } else {
                    "Error del servidor (${e.code()})."
                }
            } catch (e: Exception) {
                tvError.text = "No se pudo conectar con el servidor."
            }
            btnIngresar.isEnabled = true
        }
    }

    private fun irAPrincipal() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}