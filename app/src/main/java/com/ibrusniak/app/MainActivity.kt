package com.ibrusniak.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var button8: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onStart() {
        super.onStart()
        initLateInitVars()
        setOnClickListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_status -> {
                runRcon("status") { Log.d("RCON", it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initLateInitVars() {
        button8 = findViewById(R.id.button8)
    }

    private fun setOnClickListeners() {
        button8.setOnClickListener(::onStatusClick)
    }
    private fun onStatusClick(v: View) = runRcon("status") { Log.d("RCON", it) }

    private fun onPlayersClick(v: View) = runRcon("status") { players -> Log.d("RCON", players) }

    private fun onKickClick(v: View, nameOrId: String) = runRcon("kick \"$nameOrId\"") { Log.d("RCON", it) }

    private fun onAddBotClick(v: View) = runRcon("bot_add") { Log.d("RCON", it) }

    private fun onKickBotClick(v: View) = runRcon("bot_kick") { Log.d("RCON", it) }

    private fun onExecScriptClick(v: View, scriptName: String) = runRcon("exec $scriptName") { Log.d("RCON", it) }

    private fun runRcon(command: String, onResult: (String) -> Unit) {
        val rcon = getRcon() ?: return
        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand(command)
                onResult(result)
            } catch (e: Exception) {
                Log.e("RCON", "Ошибка: ${e.message}", e)
                onResult("Ошибка: ${e.message}")
            }
        }
    }

    private fun getRcon(): RconClient? {
        val prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE)
        val host = prefs.getString("host", "") ?: ""
        val port = prefs.getString("port", "27015")?.toIntOrNull() ?: 27015
        val password = prefs.getString("password", "") ?: ""

        if (host.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните настройки RCON", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return null
        }
        return RconClient(host, port, password)
    }
}