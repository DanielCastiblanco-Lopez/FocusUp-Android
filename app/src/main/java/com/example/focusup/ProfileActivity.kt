package com.example.focusup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.focusup.data.PomodoroStorage
import com.example.focusup.data.TaskStorage
import com.example.focusup.data.UserStorage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)



        val btnEditProfile = findViewById<android.widget.LinearLayout>(R.id.btnEditProfile)
        btnEditProfile.setOnClickListener {
            mostrarDialogoEditarPerfil()
        }

        val btnChangePassword = findViewById<android.widget.LinearLayout>(R.id.btnChangePassword)
        btnChangePassword.setOnClickListener {
            mostrarDialogoCambiarPassword()
        }

        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            UserStorage.logout(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    NavUtils.goTo(this, HomeActivity::class.java)
                    true
                }
                R.id.nav_tasks -> {
                    NavUtils.goTo(this, DashboardActivity::class.java)
                    true
                }
                R.id.nav_pomodoro -> {
                    NavUtils.goTo(this, PomodoroActivity::class.java)
                    true
                }
                R.id.nav_statistics -> {
                    NavUtils.goTo(this, StatisticsActivity::class.java)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        cargarDatosPerfil()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        val usuario = UserStorage.getCurrentUser(this)

        if (usuario == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        findViewById<TextView>(R.id.tvProfileName).text = "${usuario.nombres} ${usuario.apellidos}"
        findViewById<TextView>(R.id.tvProfileEmail).text = usuario.email

        findViewById<TextView>(R.id.tvProfileTasks).text = TaskStorage.getCompletedCount(this).toString()
        findViewById<TextView>(R.id.tvProfileMinutes).text = PomodoroStorage.getTotalMinutes(this).toString()
        findViewById<TextView>(R.id.tvProfileSessions).text = PomodoroStorage.getSessionCount(this).toString()
    }

    /** Dialogo personalizado para editar perfil, con el mismo estilo del resto de la app */
    private fun mostrarDialogoEditarPerfil() {
        val usuario = UserStorage.getCurrentUser(this) ?: return

        val dialog = DialogUtils.createCustomDialog(this, R.layout.dialog_edit_profile)
        val etNombres = dialog.findViewById<TextInputEditText>(R.id.etEditNombres)
        val etApellidos = dialog.findViewById<TextInputEditText>(R.id.etEditApellidos)
        val etEmail = dialog.findViewById<TextInputEditText>(R.id.etEditEmail)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelEditProfile)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSaveEditProfile)

        etNombres.setText(usuario.nombres)
        etApellidos.setText(usuario.apellidos)
        etEmail.setText(usuario.email)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val nuevosNombres = etNombres.text.toString().trim()
            val nuevosApellidos = etApellidos.text.toString().trim()
            val nuevoEmail = etEmail.text.toString().trim()

            if (nuevosNombres.isEmpty() || nuevosApellidos.isEmpty()) {
                Toast.makeText(this, "Nombres y apellidos no pueden estar vacios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
                Toast.makeText(this, "Correo invalido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!nuevoEmail.equals(usuario.email, ignoreCase = true) && UserStorage.emailExists(this, nuevoEmail)) {
                Toast.makeText(this, "Ese correo ya esta en uso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserStorage.updateCurrentUser(
                this,
                usuario.copy(nombres = nuevosNombres, apellidos = nuevosApellidos, email = nuevoEmail)
            )
            cargarDatosPerfil()
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    /** Dialogo personalizado para cambiar contrasena */
    private fun mostrarDialogoCambiarPassword() {
        val dialog = DialogUtils.createCustomDialog(this, R.layout.dialog_change_password)
        val etCurrentPassword = dialog.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialog.findViewById<TextInputEditText>(R.id.etNewPassword)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelChangePassword)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSaveChangePassword)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val actual = etCurrentPassword.text.toString()
            val nueva = etNewPassword.text.toString()

            if (nueva.length < 6) {
                Toast.makeText(this, "La nueva contrasena debe tener minimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exito = UserStorage.changePassword(this, actual, nueva)
            if (exito) {
                Toast.makeText(this, "Contrasena actualizada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "La contrasena actual no es correcta", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}
