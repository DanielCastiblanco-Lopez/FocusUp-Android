package com.example.focusup

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.focusup.data.UserStorage
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmailLogin)
        etPassword = findViewById(R.id.etPasswordLogin)

        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            intentarLogin()
        }

        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            mostrarDialogoRecuperarContrasena()
        }
    }

    private fun intentarLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            etPassword.requestFocus()
            return
        }

        val usuario = UserStorage.login(this, email, password)

        if (usuario == null) {
            android.widget.Toast.makeText(
                this,
                "Correo o contraseña incorrectos",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        startActivity(Intent(this, HomeActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    /** Como no hay backend/email real, permitimos recuperar verificando el correo
     * y dejando establecer una contrasena nueva directamente. */
    private fun mostrarDialogoRecuperarContrasena() {
        val input = android.widget.EditText(this)
        input.hint = "Tu correo registrado"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        android.app.AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Ingresa el correo de tu cuenta para continuar")
            .setView(input)
            .setPositiveButton("Continuar") { _, _ ->
                val correo = input.text.toString().trim()
                if (correo.isEmpty() || !UserStorage.emailExists(this, correo)) {
                    android.widget.Toast.makeText(this, "No existe una cuenta con ese correo", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                mostrarDialogoNuevaContrasena(correo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoNuevaContrasena(correo: String) {
        val input = android.widget.EditText(this)
        input.hint = "Nueva contraseña (mínimo 6 caracteres)"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        android.app.AlertDialog.Builder(this)
            .setTitle("Nueva contraseña")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaPassword = input.text.toString()
                if (nuevaPassword.length < 6) {
                    android.widget.Toast.makeText(this, "Mínimo 6 caracteres", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                UserStorage.resetPassword(this, correo, nuevaPassword)
                android.widget.Toast.makeText(this, "Contrasena actualizada, ya puedes iniciar sesion", android.widget.Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
