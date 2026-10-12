package com.jnjl.smartriego

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Backend publicado en Render (HTTPS). Debe terminar en "/".
    // Para probar con el emulador contra tu PC: "http://10.0.2.2:8000/"
    private const val BASE_URL = "https://smartriego-backend.onrender.com/"

    // Lo inicializa SmartRiegoApp al abrir la app
    lateinit var sesion: SessionManager

    // Agrega "Authorization: Bearer <token>" a cada pedido, si hay sesión.
    // Timeouts largos: el plan gratuito de Render tarda ~1 min en despertar.
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { cadena ->
            val token = sesion.obtenerToken()
            val pedido = cadena.request().newBuilder().apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            cadena.proceed(pedido)
        }
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}