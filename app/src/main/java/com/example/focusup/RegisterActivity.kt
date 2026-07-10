package com.example.focusup

import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.focusup.data.User
import com.example.focusup.data.UserStorage
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNombres: TextInputEditText
    private lateinit var etApellidos: TextInputEditText
    private lateinit var etCedula: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etFechaNacimiento: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etNombres = findViewById(R.id.etNombres)
        etApellidos = findViewById(R.id.etApellidos)
        etCedula = findViewById(R.id.etCedula)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)

        etFechaNacimiento.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val meses = arrayOf(
                        "ene", "feb", "mar", "abr", "may", "jun",
                        "jul", "ago", "sep", "oct", "nov", "dic"
                    )
                    etFechaNacimiento.setText("$dayOfMonth ${meses[month]} $year")
                },
                calendar.get(java.util.Calendar.YEAR) - 18,
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            intentarRegistro()
        }

        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)
        tvGoLogin.setOnClickListener {
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    private fun intentarRegistro() {
        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val cedula = etCedula.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val fechaNacimiento = etFechaNacimiento.text.toString().trim()

        if (nombres.isEmpty()) {
            etNombres.error = "Ingresa tus nombres"
            etNombres.requestFocus()
            return
        }
        if (apellidos.isEmpty()) {
            etApellidos.error = "Ingresa tus apellidos"
            etApellidos.requestFocus()
            return
        }
        if (cedula.isEmpty() || cedula.length < 6) {
            etCedula.error = "Ingresa una cedula valida"
            etCedula.requestFocus()
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo valido"
            etEmail.requestFocus()
            return
        }
        if (password.length < 6) {
            etPassword.error = "Minimo 6 caracteres"
            etPassword.requestFocus()
            return
        }
        if (fechaNacimiento.isEmpty()) {
            android.widget.Toast.makeText(this, "Selecciona tu fecha de nacimiento", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        if (UserStorage.emailExists(this, email)) {
            etEmail.error = "Este correo ya esta registrado"
            etEmail.requestFocus()
            android.widget.Toast.makeText(this, "Ya existe una cuenta con ese correo. Inicia sesion.", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        val nuevoUsuario = User(
            nombres = nombres,
            apellidos = apellidos,
            cedula = cedula,
            email = email,
            password = password,
            fechaNacimiento = fechaNacimiento
        )

        UserStorage.registerUser(this, nuevoUsuario)

        startActivity(android.content.Intent(this, HomeActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
