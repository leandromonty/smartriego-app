package com.jnjl.smartriego

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardarToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun obtenerToken(): String? = prefs.getString("token", null)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}