package com.ibrusniak.app

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
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

    private var isRequestInProgress = false

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var etCommand: TextView
    private lateinit var btnSend: Button

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

        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        tvLog.setHorizontallyScrolling(true)
        tvLog.movementMethod = null

        etCommand = findViewById<EditText>(R.id.etCommand)
        btnSend = findViewById<Button>(R.id.btnSend)

        btnSend.setOnClickListener {
            val cmd = etCommand.text.toString().trim()
            sendCustomCommand(cmd)
        }
        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val cmd = etCommand.text.toString().trim()
                sendCustomCommand(cmd)
                true
            } else false
        }

        savedInstanceState?.getString("log_text")?.let { savedLog ->
            tvLog.text = savedLog
            scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("log_text", tvLog.text.toString())
    }

    fun sendCustomCommand(cmd: String) {
        if (cmd.isNotEmpty()) {
            appendLog("> $cmd")
            runRcon(cmd) { appendLog(it) }
        }
    }

    override fun onStart() {
        super.onStart()
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
                sendCustomCommand("Status")
                true
            }
            R.id.action_bot_kick_all -> {
                sendCustomCommand("bot_kick")
                true
            }
            R.id.action_bot_add_t -> {
                sendCustomCommand("bot_add_t")
                true
            }
            R.id.action_bot_add_ct -> {
                sendCustomCommand("bot_add_ct")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun onStatusClick(v: View) = runRcon("status") { Log.d("RCON", it) }

    private fun onPlayersClick(v: View) = runRcon("status") { players -> Log.d("RCON", players) }

    private fun onKickClick(v: View, nameOrId: String) = runRcon("kick \"$nameOrId\"") { Log.d("RCON", it) }

    private fun onAddBotClick(v: View) = runRcon("bot_add") { Log.d("RCON", it) }

    private fun onKickBotClick(v: View) = runRcon("bot_kick") { Log.d("RCON", it) }

    private fun onExecScriptClick(v: View, scriptName: String) = runRcon("exec $scriptName") { Log.d("RCON", it) }

    private fun runRcon(command: String, onResult: (String) -> Unit) {
        if (isRequestInProgress) {
            Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
            return
        }

        val rcon = getRcon() ?: return
        isRequestInProgress = true
        setUiEnabled(false)

        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand(command)
                if (result == "AUTH_FAILED") {
                    onResult("❌ fail\nНеверный RCON пароль")
                } else {
                    val body = if (result.isNotBlank()) "\n$result" else ""
                    onResult("✅ successful$body")
                }
            } catch (e: Exception) {
                Log.e("RCON", "Ошибка: ${e.message}", e)
                onResult("❌ fail\n${e.message}")
            } finally {
                isRequestInProgress = false
                setUiEnabled(true)
            }
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        findViewById<EditText>(R.id.etCommand).isEnabled = enabled
        findViewById<Button>(R.id.btnSend).isEnabled = enabled
        invalidateOptionsMenu() // чтобы пункты тулбар-меню тоже перерисовались с учётом isEnabled ниже
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

    private fun appendLog(text: String) {
        runOnUiThread {
            tvLog.append("$text\n\n")
            scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
        }
    }
}