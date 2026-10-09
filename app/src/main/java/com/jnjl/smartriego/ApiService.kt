package com.jnjl.smartriego

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // NUEVO: login contra el backend real
    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequest): LoginResponse

    // VIEJO (backend de prueba): se reemplaza en el paso 3
    @GET("lectura/ultima")
    suspend fun obtenerUltimaLectura(): Lectura

    @GET("lectura/historial")
    suspend fun obtenerHistorial(): List<Registro>

    @POST("riego/manual")
    suspend fun regarManual(): Lectura
}