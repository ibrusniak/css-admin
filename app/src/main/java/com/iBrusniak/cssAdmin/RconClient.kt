package com.iBrusniak.cssAdmin

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RconClient(
    private val host: String,
    private val port: Int,
    private val password: String
) {
    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 20000)
            socket.soTimeout = 20000
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            fun sendPacket(id: Int, type: Int, body: String) {
                val b = body.toByteArray(Charsets.UTF_8)
                val size = 4 + 4 + b.size + 2
                val buf = ByteBuffer.allocate(4 + size).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(size); buf.putInt(id); buf.putInt(type)
                buf.put(b); buf.put(0); buf.put(0)
                out.write(buf.array()); out.flush()
            }

            fun readPacket(): Triple<Int, Int, String> {
                val sizeBuf = ByteArray(4)
                input.readFully(sizeBuf)
                val size = ByteBuffer.wrap(sizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
                val packet = ByteArray(size)
                input.readFully(packet)
                val bb = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
                val id = bb.int; val type = bb.int
                val bodySize = (size - 10).coerceAtLeast(0)
                val body = ByteArray(bodySize)
                bb.get(body)
                return Triple(id, type, String(body, Charsets.UTF_8).trimEnd('\u0000'))
            }

            // Auth
            sendPacket(1, 3, password)
            // Skip SERVERDATA_RESPONSE_VALUE if any
            var authResponse = readPacket()
            if (authResponse.second == 0) { // SERVERDATA_RESPONSE_VALUE
                authResponse = readPacket() // Should be SERVERDATA_AUTH_RESPONSE
            }
            if (authResponse.first == -1) return@use "AUTH_FAILED"

            // Command
            val commandId = 2
            val terminatorId = 3
            sendPacket(commandId, 2, command)
            sendPacket(terminatorId, 2, "") // Dummy packet to detect end of response

            val fullResponse = StringBuilder()
            while (true) {
                val (id, type, body) = readPacket()
                if (id == terminatorId) break
                if (id == commandId && type == 0) {
                    fullResponse.append(body)
                }
            }
            fullResponse.toString()
        }
    }
}