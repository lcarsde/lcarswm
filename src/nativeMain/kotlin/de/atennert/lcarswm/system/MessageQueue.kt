package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.O_CREAT
import platform.posix.O_NONBLOCK
import platform.posix.O_WRONLY

class MessageQueue(private val posixApi: PosixApi, private val name: String) {

    private val oFlags = O_WRONLY or O_NONBLOCK or O_CREAT
    private val queuePermissions = 432 // 0660

    private val maxMessageCount = 10
    private val maxMessageSize = 1024

    private val mqDes: mqd_t

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

    fun close() {
        if (posixApi.mqClose(mqDes) == -1 ) {
            return
        }

        posixApi.mqUnlink(name)
    }
}