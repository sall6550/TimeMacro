package com.example.timemacro

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SntpClient {
    private var currentTimeMillis: Long = 0

    fun requestTime(host: String, timeout: Int): Boolean {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = timeout
            val address = InetAddress.getByName(host)
            
            val buffer = ByteArray(48)
            buffer[0] = 0x1B // NTP request version 3

            val request = DatagramPacket(buffer, buffer.size, address, 123)
            val response = DatagramPacket(buffer, buffer.size)

            socket.send(request)
            socket.receive(response)

            // NTP 타임스탬프는 1900년부터 시작하므로, Unix 시간(1970년)으로 변환
            val seconds = getLong(buffer, 40)
            val fraction = getLong(buffer, 44)
            
            currentTimeMillis = ((seconds - 2208988800L) * 1000) + (fraction * 1000L / 0x100000000L)
            
            socket.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun getLong(bytes: ByteArray, offset: Int): Long {
        var result: Long = 0
        for (i in 0..3) {
            result = result shl 8
            result = result or (bytes[offset + i].toLong() and 0xFF)
        }
        return result
    }

    fun currentTimeMillis(): Long = currentTimeMillis
} 