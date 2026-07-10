package com.example.focusup.data

/**
 * Representa una cuenta de usuario registrada en la app.
 * NOTA: la contrasena se guarda en texto plano en SharedPreferences,
 * lo cual es aceptable para un proyecto academico local, pero NO para
 * produccion real (ahi se necesitaria hashing + backend).
 */
data class User(
    val nombres: String,
    val apellidos: String,
    val cedula: String,
    val email: String,
    val password: String,
    val fechaNacimiento: String
)
