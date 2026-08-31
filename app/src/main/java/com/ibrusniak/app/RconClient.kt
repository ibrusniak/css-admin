package com.ibrusniak.app

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
            socket.connect(InetSocketAddress(host, port), 3000)
            socket.soTimeout = 3000
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            fun send(id: Int, type: Int, body: String) {
                val b = body.toByteArray(Charsets.UTF_8)
                val size = 4 + 4 + b.size + 2
                val buf = ByteBuffer.allocate(4 + size).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(size); buf.putInt(id); buf.putInt(type)
                buf.put(b); buf.put(0); buf.put(0)
                out.write(buf.array()); out.flush()
            }

            fun readOne(): Triple<Int, Int, String> {
                val sizeBuf = ByteArray(4)
                input.readFully(sizeBuf)
                val size = ByteBuffer.wrap(sizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
                val packet = ByteArray(size)
                input.readFully(packet)
                val bb = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
                val id = bb.int; val type = bb.int
                val body = ByteArray(size - 10)
                bb.get(body)
                return Triple(id, type, String(body, Charsets.UTF_8))
            }

            send(1, 3, password)
            readOne()
            val (authId, _, _) = readOne()
            if (authId == -1) return@use "AUTH_FAILED"

            send(2, 2, command)
            val (_, _, response) = readOne()
            response
        }
    }
}