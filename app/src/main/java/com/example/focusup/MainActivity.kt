package com.example.focusup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.focusup.data.UserStorage

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesion activa, saltamos directo al Home
        val destino = if (UserStorage.isLoggedIn(this)) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, destino))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
