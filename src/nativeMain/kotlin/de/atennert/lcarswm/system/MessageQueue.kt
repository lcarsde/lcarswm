package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.*

class MessageQueue(private val posixApi: PosixApi, private val name: String, private val mode: Mode) {

    enum class Mode (val flag: Int) {
        READ(O_RDONLY),
        WRITE(O_WRONLY),
        READ_WRITE(O_RDWR)
    }

    private val oFlags = mode.flag or O_NONBLOCK or O_CREAT
    private val queuePermissions = 432 // 0660

    private val maxMessageCount = 10
    private val maxMessageSize = 1024

    private val mqDes: mqd_t

    private var receiveJob: Job? = null

    init {
        val mqAttributes = nativeHeap.alloc<mq_attr>()
        mqAttributes.mq_flags = O_NONBLOCK.convert()
        mqAttributes.mq_maxmsg = maxMessageCount.convert()
        mqAttributes.mq_msgsize = maxMessageSize.convert()
        mqAttributes.mq_curmsgs = 0

        mqDes = posixApi.mqOpen(name, oFlags, queuePermissions.convert(), mqAttributes.ptr)
    }

    fun sendMessage(message: String) {
        // TODO split to long messages
        posixApi.mqSend(mqDes, message, 0.convert())
    }

    fun receiveMessage(receiver: (String) -> Unit) {
        assert(mode == Mode.READ || mode == Mode.READ_WRITE)

        receiveJob = GlobalScope.launch {
            while (true) {
                val msgBuffer = ByteArray(maxMessageSize).pin()
                val msgSize = posixApi.mqReceive(mqDes, msgBuffer.addressOf(0), maxMessageSize.convert(), null)
                // TODO read message
                delay(400)
            }
        }
    }

    private fun stopReceiving() {
        receiveJob?.let { job ->
            runBlocking(GlobalScope.coroutineContext) {
                job.cancelAndJoin()
            }
        }
    }

    fun close() {
        stopReceiving()

        if (posixApi.mqClose(mqDes) == -1 ) {
            return
        }

        posixApi.mqUnlink(name)
    }
}