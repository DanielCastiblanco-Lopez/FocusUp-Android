package com.example.focusup.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda las cuentas registradas y controla la sesion activa,
 * todo en SharedPreferences como JSON (sin base de datos).
 */
object UserStorage {

    private const val PREFS_NAME = "focusup_prefs"
    private const val KEY_USERS = "users_json"
    private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    private const val KEY_FIRST_USE_DATE = "first_use_date"

    private fun getUsers(context: Context): MutableList<User> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USERS, null) ?: return mutableListOf()

        val list = mutableListOf<User>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                User(
                    nombres = obj.getString("nombres"),
                    apellidos = obj.getString("apellidos"),
                    cedula = obj.getString("cedula"),
                    email = obj.getString("email"),
                    password = obj.getString("password"),
                    fechaNacimiento = obj.getString("fechaNacimiento")
                )
            )
        }
        return list
    }

    private fun saveUsers(context: Context, users: List<User>) {
        val array = JSONArray()
        for (u in users) {
            val obj = JSONObject()
            obj.put("nombres", u.nombres)
            obj.put("apellidos", u.apellidos)
            obj.put("cedula", u.cedula)
            obj.put("email", u.email)
            obj.put("password", u.password)
            obj.put("fechaNacimiento", u.fechaNacimiento)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USERS, array.toString()).apply()
    }

    /** true si el correo ya esta registrado */
    fun emailExists(context: Context, email: String): Boolean =
        getUsers(context).any { it.email.equals(email, ignoreCase = true) }

    /** Registra un usuario nuevo. Devuelve false si el correo ya existe. */
    fun registerUser(context: Context, user: User): Boolean {
        if (emailExists(context, user.email)) return false
        val users = getUsers(context)
        users.add(user)
        saveUsers(context, users)
        setLoggedInUser(context, user.email)
        markFirstUseIfNeeded(context)
        return true
    }

    /** Guarda la fecha en que el usuario empezo a usar la app (solo la primera vez) */
    fun markFirstUseIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FIRST_USE_DATE)) {
            prefs.edit().putLong(KEY_FIRST_USE_DATE, System.currentTimeMillis()).apply()
        }
    }

    /** Devuelve la fecha de inicio formateada, ej: "15 de junio de 2026" */
    fun getFirstUseDateFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val millis = prefs.getLong(KEY_FIRST_USE_DATE, System.currentTimeMillis())
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        val meses = arrayOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} de ${meses[cal.get(java.util.Calendar.MONTH)]} de ${cal.get(java.util.Calendar.YEAR)}"
    }

    /** Intenta iniciar sesion. Devuelve el usuario si las credenciales son correctas, o null. */
    fun login(context: Context, email: String, password: String): User? {
        val user = getUsers(context).find {
            it.email.equals(email, ignoreCase = true) && it.password == password
        }
        if (user != null) {
            setLoggedInUser(context, user.email)
            markFirstUseIfNeeded(context)
        }
        return user
    }

    private fun setLoggedInUser(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email).apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
    }

    /** Devuelve el usuario actualmente logueado, o null si no hay sesion activa */
    fun getCurrentUser(context: Context): User? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null) ?: return null
        return getUsers(context).find { it.email.equals(email, ignoreCase = true) }
    }

    fun isLoggedIn(context: Context): Boolean = getCurrentUser(context) != null

    /** Actualiza los datos de perfil del usuario actualmente logueado */
    fun updateCurrentUser(context: Context, updated: User) {
        val users = getUsers(context)
        val currentEmail = getCurrentUser(context)?.email ?: return
        val index = users.indexOfFirst { it.email.equals(currentEmail, ignoreCase = true) }
        if (index != -1) {
            users[index] = updated
            saveUsers(context, users)
            setLoggedInUser(context, updated.email)
        }
    }

    /** Cambia la contrasena del usuario actual. Devuelve false si la actual no coincide. */
    fun changePassword(context: Context, currentPassword: String, newPassword: String): Boolean {
        val user = getCurrentUser(context) ?: return false
        if (user.password != currentPassword) return false
        updateCurrentUser(context, user.copy(password = newPassword))
        return true
    }

    /** Cambia la contrasena de una cuenta por correo, sin necesidad de sesion activa
     * (usado en el flujo de "Olvidaste tu contrasena"). */
    fun resetPassword(context: Context, email: String, newPassword: String): Boolean {
        val users = getUsers(context)
        val index = users.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (index == -1) return false
        users[index] = users[index].copy(password = newPassword)
        saveUsers(context, users)
        return true
    }
}
