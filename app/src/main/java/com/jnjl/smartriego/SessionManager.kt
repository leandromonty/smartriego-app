package com.jnjl.smartriego

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardarToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun obtenerToken(): String? = prefs.getString("token", null)

    fun guardarEquipo(id: Int) {
        prefs.edit().putInt("equipo_id", id).apply()
    }

    fun obtenerEquipo(): Int = prefs.getInt("equipo_id", -1)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}