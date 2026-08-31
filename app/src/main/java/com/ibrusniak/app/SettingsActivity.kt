package com.ibrusniak.app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE)

        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etPassword = findViewById(R.id.etPassword)

        etHost.setText(prefs.getString("host", ""))
        etPort.setText(prefs.getString("port", "27015"))
        etPassword.setText(prefs.getString("password", ""))

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("host", etHost.text.toString().trim())
                .putString("port", etPort.text.toString().trim())
                .putString("password", etPassword.text.toString().trim())
                .apply()
            finish()
        }
    }
}