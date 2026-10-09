package com.jnjl.smartriego

data class LoginRequest(
    val email: String,
    val password: String
)

data class UsuarioApp(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val usuario: UsuarioApp
)

data class DispositivoApp(
    val id: Int,
    val nombre: String,
    val codigo_activacion: String
)