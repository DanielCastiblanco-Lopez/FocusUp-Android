package com.example.focusup.data

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val deadline: String,        // texto mostrado, ej: "20 jun 2026"
    val deadlineMillis: Long = 0L,  // fecha real para comparar con el calendario
    val priority: String,        // "Alta", "Media" o "Baja"
    val isDone: Boolean = false,
    val completedAtMillis: Long = 0L,  // momento exacto en que se marco como completada
    val note: String = ""        // nota/contexto opcional sobre la tarea
)
