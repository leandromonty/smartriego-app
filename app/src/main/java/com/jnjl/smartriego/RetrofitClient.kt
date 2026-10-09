package com.jnjl.smartriego

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 10.0.2.2 es la forma en que el emulador "ve" tu PC
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // Lo inicializa SmartRiegoApp al abrir la app
    lateinit var sesion: SessionManager

    // Agrega "Authorization: Bearer <token>" a cada pedido, si hay sesión
    private val cliente = OkHttpClient.Builder()
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