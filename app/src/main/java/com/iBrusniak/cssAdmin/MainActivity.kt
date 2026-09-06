package com.iBrusniak.cssAdmin

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.text.SpannableString
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
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
import androidx.core.content.edit
import androidx.appcompat.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.view.animation.AnimationUtils

class MainActivity : AppCompatActivity() {

    private var isRequestInProgress = false
    private lateinit var loadingIndicator: View

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView

    private lateinit var etCommand: AutoCompleteTextView
    private lateinit var commandHistoryAdapter: ArrayAdapter<String>
    private val historyPrefs by lazy { getSharedPreferences("command_history", MODE_PRIVATE) }
    private val commandHistory = mutableListOf<String>()

    private lateinit var btnSend: Button

    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var button5: Button
    private lateinit var button6: Button
    private lateinit var button7: Button
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
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            toolbar.subtitle = "v${pInfo.versionName}"
        } catch (_: Exception) {}

        loadingIndicator = findViewById(R.id.loadingIndicator)

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
            showMapSelectionDialog()
        }

        button7.setOnClickListener {
            showPlayerKickDialog()
        }

        button8.setOnClickListener {}


        etCommand = findViewById(R.id.etCommand)

        loadCommandHistory()
        commandHistoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        etCommand.setAdapter(commandHistoryAdapter)
        updateHistoryAdapter()

        etCommand.setOnClickListener {
            if (commandHistory.isNotEmpty()) {
                etCommand.showDropDown()
            }
        }
        etCommand.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && commandHistory.isNotEmpty()) {
                etCommand.showDropDown()
            }
        }

        btnSend = findViewById(R.id.btnSend)

        btnSend.setOnClickListener {
            val cmd = etCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                addToHistory(cmd)
                runRCONCommand(cmd)
                etCommand.text.clear()
                hideKeyboard()
            }
        }

        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val cmd = etCommand.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    addToHistory(cmd)
                    runRCONCommand(cmd)
                    etCommand.text.clear()
                    hideKeyboard()
                }
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
            runRCONCommand(cmd)
        }
    }

    override fun onStart() {
        super.onStart()
        loadCommandHistory()
        updateHistoryAdapter()
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun runRCONCommand(command: String) {

        if (isRequestInProgress) {
            Toast.makeText(
                this, getString(R.string.please_wait), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rcon = getRcon() ?: return
        isRequestInProgress = true
        setLoading(true)

        appendLog(">>> $command")

        if (command.uppercase().trim() == "QUIT" || command.uppercase().trim() == "EXIT") {
            appendLog("${getString(R.string.fail)}\n${getString(R.string.prohibited)}\n")
            isRequestInProgress = false
            return
        }

        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand(command)
                if (result == "AUTH_FAILED") {
                    appendLog("${getString(R.string.fail)}\n${getString(R.string.invalid_rcon_password)}\n")
                } else {
                    val body = if (result.isNotEmpty() && result.isNotBlank()) result else ""
                    appendLog("${getString(R.string.successful)}\n$body")
                }
            } catch (e: Exception) {
                appendLog("${getString(R.string.fail)}\n${e.message}\n")
            } finally {
                isRequestInProgress = false
                setLoading(false)
            }
        }
    }

    private fun loadCommandHistory() {
        val saved = historyPrefs.getString("history", "") ?: ""
        commandHistory.clear()
        if (saved.isNotEmpty()) {
            commandHistory.addAll(saved.split("\u0001").filter { it.isNotBlank() })
        }
    }

    private fun saveCommandHistory() {
        historyPrefs.edit(commit = true) {
            putString("history", commandHistory.joinToString("\u0001"))
        }
    }

    private fun addToHistory(command: String) {

        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        commandHistory.remove(trimmed)
        commandHistory.add(0, trimmed)

        while (commandHistory.size > 15) {
            commandHistory.removeAt(commandHistory.size - 1)
        }

        saveCommandHistory()
        updateHistoryAdapter()
    }

    private fun updateHistoryAdapter() {
        if (::commandHistoryAdapter.isInitialized) {
            commandHistoryAdapter.clear()
            commandHistoryAdapter.addAll(commandHistory)
            commandHistoryAdapter.notifyDataSetChanged()
        }
    }

    private fun getRcon(): RconClient? {
        val prefs = getSharedPreferences("rcon_prefs", MODE_PRIVATE)
        val host = prefs.getString("host", "") ?: ""
        val port = prefs.getString("port", "27015")?.toIntOrNull() ?: 27015
        val password = prefs.getString("password", "") ?: ""

        if (host.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_rcon_settings), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return null
        }
        return RconClient(host, port, password)
    }

    private fun appendLog(text: String) {
        runOnUiThread {
            val spannable = SpannableString(text + "\n")
            tvLog.append(spannable)
            scrollLog.post {
                val scrollPos = (tvLog.height - tvLog.paddingBottom - scrollLog.height).coerceAtLeast(0)
                scrollLog.scrollTo(0, scrollPos)
            }
        }
    }

    private fun showMapSelectionDialog() {
        if (isRequestInProgress) {
            Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
            return
        }

        val rcon = getRcon() ?: return
        isRequestInProgress = true
        setLoading(true)

        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand("maps *")
                appendLog(">>> maps *\n$result")
                val maps = parseMaps(result)
                if (maps.isEmpty()) {
                    Toast.makeText(this@MainActivity, getString(R.string.fail), Toast.LENGTH_SHORT).show()
                    appendLog(">>> maps *\n${getString(R.string.fail)}\nNo maps found in response.")
                } else {
                    showDialog(maps)
                }
            } catch (e: Exception) {
                appendLog("${getString(R.string.fail)}\n${e.message}\n")
            } finally {
                isRequestInProgress = false
                setLoading(false)
            }
        }
    }

    private fun parseMaps(rconOutput: String): List<String> {
        val regex = """\(fs\)\s+([^\s\n\r]+)""".toRegex()
        return regex.findAll(rconOutput)
            .map { it.groupValues[1] }
            .filter { mapName ->
                !mapName.startsWith("test_", ignoreCase = true) &&
                        !mapName.contains("_test", ignoreCase = true)
            }
            .toList()
            .distinct()
            .sorted()
    }

    private fun showDialog(maps: List<String>) {
        var selectedMap = maps[0]
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_map_title))
            .setSingleChoiceItems(maps.toTypedArray(), 0) { _, which ->
                selectedMap = maps[which]
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                runRCONCommand("changelevel $selectedMap")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPlayerKickDialog() {
        if (isRequestInProgress) {
            Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
            return
        }

        val rcon = getRcon() ?: return
        isRequestInProgress = true
        setLoading(true)

        lifecycleScope.launch {
            try {
                val result = rcon.sendCommand("users")
                appendLog(">>> users\n$result")
                val players = parsePlayers(result)
                if (players.isEmpty()) {
                    Toast.makeText(this@MainActivity, getString(R.string.no_players), Toast.LENGTH_SHORT).show()
                } else {
                    showKickDialog(players)
                }
            } catch (e: Exception) {
                appendLog("${getString(R.string.fail)}\n${e.message}\n")
            } finally {
                isRequestInProgress = false
                setLoading(false)
            }
        }
    }

    private data class Player(val userId: String, val name: String)

    private fun parsePlayers(rconOutput: String): List<Player> {
        val players = mutableListOf<Player>()
        val regex = """\d+:(\d+):"([^"]+)"""".toRegex()
        regex.findAll(rconOutput).forEach { match ->
            val userId = match.groupValues[1]
            val name = match.groupValues[2]
            players.add(Player(userId, name))
        }
        return players.sortedBy { it.name.lowercase() }
    }

    private fun showKickDialog(players: List<Player>) {
        val names = players.map { it.name }.toTypedArray()
        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_player_title))
            .setSingleChoiceItems(names, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                runRCONCommand("kickid ${players[selectedIndex].userId}")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etCommand.windowToken, 0)
        etCommand.clearFocus()
    }

    private fun setLoading(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                loadingIndicator.visibility = View.VISIBLE
                val blink = AnimationUtils.loadAnimation(this, R.anim.blink_animation)
                loadingIndicator.startAnimation(blink)
            } else {
                loadingIndicator.clearAnimation()
                loadingIndicator.visibility = View.GONE
            }
        }
    }
}
