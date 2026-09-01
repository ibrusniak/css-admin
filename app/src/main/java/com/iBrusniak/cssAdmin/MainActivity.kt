package com.iBrusniak.cssAdmin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private val COLOR_COMMAND = "#FFD700".toColorInt()
    private val COLOR_SUCCESS = "#00FF00".toColorInt()
    private val COLOR_ERROR = "#FF5555".toColorInt()

    private var isRequestInProgress = false

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var etCommand: TextView
    private lateinit var btnSend: Button

    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var button5: Button
    private lateinit var button6: Button
    private lateinit var button7: Button
    private lateinit var button8: Button
    private lateinit var button9: Button
    private lateinit var button10: Button
    private lateinit var button11: Button
    private lateinit var button12: Button

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

        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3)
        button4 = findViewById(R.id.button4)
        button5 = findViewById(R.id.button5)
        button6 = findViewById(R.id.button6)
        button7 = findViewById(R.id.button7)
        button8 = findViewById(R.id.button8)
        button9 = findViewById(R.id.button9)
        button10 = findViewById(R.id.button10)
        button11 = findViewById(R.id.button11)
        button12 = findViewById(R.id.button12)

        button1.setOnClickListener {
            sendCustomCommand("Status")
        }

        button2.setOnClickListener {
            sendCustomCommand("bot_kick")
        }

        button3.setOnClickListener {
            sendCustomCommand("bot_add_ct")
        }

        button4.setOnClickListener {
            sendCustomCommand("bot_add_t")
        }

        button5.setOnClickListener {
            sendCustomCommand("mp_restartgame 5")
        }

        button6.setOnClickListener {

            appendLog("> ## player names:", COLOR_COMMAND)
            val rcon = getRcon() ?: return@setOnClickListener
            isRequestInProgress = true
            setUiEnabled(false)
            lifecycleScope.launch {
                try {
                    val result = rcon.sendCommand("status")
                    val names = parsePlayerNames(result)
                    val text = if (names.isEmpty()) "No players\n" else names.joinToString("\n") { "• $it" } + "\n"
                    appendLog("✅ successful\n$text", COLOR_SUCCESS)
                } catch (e: Exception) {
                    appendLog("❌ fail\n${e.message}", COLOR_ERROR)
                } finally {
                    isRequestInProgress = false
                    setUiEnabled(true)
                }
            }
        }

        button7.setOnClickListener {}

        button8.setOnClickListener {}

        button9.setOnClickListener {}

        button10.setOnClickListener {}

        button11.setOnClickListener {}

        button12.setOnClickListener {}

        etCommand = findViewById<EditText>(R.id.etCommand)
        btnSend = findViewById(R.id.btnSend)

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

    private fun parsePlayerNames(statusResponse: String): List<String> {
        val regex = Regex("""^#\s*\d+\s+"([^"]+)"""", RegexOption.MULTILINE)
        return regex.findAll(statusResponse)
            .map { it.groupValues[1] }
            .toList()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("log_text", tvLog.text.toString())
    }

    fun sendCustomCommand(cmd: String) {
        if (cmd.isNotEmpty()) {
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

            R.id.action_restart_game -> {
                sendCustomCommand("mp_restartgame 5")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun runRcon(command: String, onResult: (String) -> Unit) {

        if (isRequestInProgress) {
            Toast.makeText(
                this, "Please wait for the previous command to finish", Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rcon = getRcon() ?: return
        isRequestInProgress = true
        setUiEnabled(false)

        appendLog("> $command", COLOR_COMMAND)

        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand(command)
                if (result == "AUTH_FAILED") {
                    appendLog("❌ fail\nInvalid RCON password", COLOR_ERROR)
                } else {
                    val body = if (result.isNotEmpty() && result.isNotBlank()) result else ""
                    appendLog("✅ successful\n$body", COLOR_SUCCESS)
                }
            } catch (e: Exception) {
                appendLog("❌ fail\n${e.message}\n", COLOR_ERROR)
            } finally {
                isRequestInProgress = false
                setUiEnabled(true)
            }
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        findViewById<EditText>(R.id.etCommand).isEnabled = enabled
        findViewById<Button>(R.id.btnSend).isEnabled = enabled
        invalidateOptionsMenu()
    }

    private fun getRcon(): RconClient? {
        val prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE)
        val host = prefs.getString("host", "") ?: ""
        val port = prefs.getString("port", "27015")?.toIntOrNull() ?: 27015
        val password = prefs.getString("password", "") ?: ""

        if (host.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill in RCON settings", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return null
        }
        return RconClient(host, port, password)
    }

    private fun appendLog(text: String, color: Int = Color.parseColor("#00FF00")) {
        runOnUiThread {
            val spannable = SpannableString(text + "\n")
            spannable.setSpan(
                ForegroundColorSpan(color), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvLog.append(spannable)
            scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
        }
    }
}