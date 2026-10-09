package com.jnjl.smartriego

data class Lectura(
    val humedad: Int,
    val bomba_encendida: Boolean,
    val deposito_bajo: Boolean = false,
    val riego_pendiente: Boolean = false
)