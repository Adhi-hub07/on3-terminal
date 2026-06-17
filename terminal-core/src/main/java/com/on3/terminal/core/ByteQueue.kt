package com.on3.terminal.core

class ByteQueue(private val capacity: Int) {
    private val buffer = ByteArray(capacity)
    private var head = 0
    private var size = 0
    private var closed = false

    @Synchronized
    fun write(data: ByteArray, offset: Int, count: Int): Boolean {
        if (closed || count == 0) return false
        var written = 0
        while (written < count) {
            while (size == capacity) {
                if (closed) return false
                (this as Object).wait(500)
                if (closed) return false
            }
            val tail = (head + size) % capacity
            val space = capacity - size
            val toWrite = minOf(space, count - written)
            val firstPart = minOf(toWrite, capacity - tail)
            System.arraycopy(data, offset + written, buffer, tail, firstPart)
            if (toWrite > firstPart) {
                System.arraycopy(data, offset + written + firstPart, buffer, 0, toWrite - firstPart)
            }
            size += toWrite
            written += toWrite
            (this as Object).notifyAll()
        }
        return true
    }

    @Synchronized
    fun read(outBuffer: ByteArray, block: Boolean): Int {
        if (size == 0 && closed) return -1
        while (size == 0) {
            if (closed) return -1
            if (!block) return 0
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
                return -1
            }
        }
        val count = minOf(outBuffer.size, size)
        val firstPart = minOf(count, capacity - head)
        System.arraycopy(buffer, head, outBuffer, 0, firstPart)
        if (count > firstPart) {
            System.arraycopy(buffer, 0, outBuffer, firstPart, count - firstPart)
        }
        head = (head + count) % capacity
        size -= count
        (this as Object).notifyAll()
        return count
    }

    @Synchronized
    fun close() {
        closed = true
        (this as Object).notifyAll()
    }
}
