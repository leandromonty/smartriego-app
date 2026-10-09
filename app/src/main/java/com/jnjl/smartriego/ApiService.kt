package com.jnjl.smartriego

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequest): LoginResponse

    @GET("dispositivos")
    suspend fun obtenerDispositivos(): List<DispositivoApp>

    @GET("dispositivos/{id}/lectura/ultima")
    suspend fun obtenerUltimaLectura(@Path("id") id: Int): Lectura

    @GET("dispositivos/{id}/lectura/historial")
    suspend fun obtenerHistorial(@Path("id") id: Int): List<Registro>

    @POST("dispositivos/{id}/riego/manual")
    suspend fun regarManual(@Path("id") id: Int): Lectura
}