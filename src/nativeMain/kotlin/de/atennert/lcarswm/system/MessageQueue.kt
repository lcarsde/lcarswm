package de.atennert.lcarswm.system

import de.atennert.lcarswm.lifecycle.closeWith
import kotlinx.cinterop.*
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.*

/**
 * Adapter class for Posix message queue. The message queue will be created and destroyed
 * by this implementation. Other end points must connect later to the queue and disconnect
 * before it is destroyed.
 *
 * @param name The name of the message queue, unique identifier for each queue
 * @param mode The usage mode for this queue in this app: READ, WRITE or READ_WRITE
 */
@ExperimentalForeignApi
class MessageQueue(private val name: String, private val mode: Mode) {

    enum class Mode (val flag: Int) {
        READ(O_RDONLY),
        WRITE(O_WRONLY),
        READ_WRITE(O_RDWR)
    }

    private val oFlags = mode.flag or O_NONBLOCK or O_CREAT
    private val queuePermissions: mode_t = 432.convert() // 0660

    private val maxMessageCount = 10
    private val maxMessageSize = 1024

    private val mqDes: mqd_t

    init {
        val mqAttributes = nativeHeap.alloc<mq_attr>()
        mqAttributes.mq_flags = O_NONBLOCK.convert()
        mqAttributes.mq_maxmsg = maxMessageCount.convert()
        mqAttributes.mq_msgsize = maxMessageSize.convert()
        mqAttributes.mq_curmsgs = 0

        mqDes = wrapMqOpen(name, oFlags, queuePermissions, mqAttributes)

        closeWith(MessageQueue::close)
    }

    fun sendMessage(message: String) {
        assert(mode == Mode.WRITE || mode == Mode.READ_WRITE)

        wrapMqSend(mqDes, message, message.length.convert(), 0.convert())
    }

    fun receiveMessage(): String? {
        assert(mode == Mode.READ || mode == Mode.READ_WRITE)

        val msgBuffer = ByteArray(maxMessageSize)
        var msgSize: Long = -1
        msgBuffer.usePinned {
            msgSize = wrapMqReceive(mqDes, it.addressOf(0), maxMessageSize.convert(), null)
        }
        if (msgSize > 0) {
            return msgBuffer.decodeToString(0, msgSize.convert())
        }
        return null
    }

    private fun close() {
        if (wrapMqClose(mqDes) == -1 ) {
            return
        }

        wrapMqUnlink(name)
    }
}