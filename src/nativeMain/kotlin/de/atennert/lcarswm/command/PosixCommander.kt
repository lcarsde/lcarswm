package de.atennert.lcarswm.command

import kotlinx.cinterop.*
import platform.posix.*

class PosixCommander : Commander() {
    override fun run(command: List<String>): Boolean {
        when (fork()) {
            -1 -> return false
            0 -> {
                if (setsid() == -1) {
                    perror("setsid failed")
                    exit(1)
                }

                if (!execute(command[0], command)) {
                    perror("execvp failed")
                    exit(1)
                }

                exit(0)
            }
        }
        return true
    }

    private fun execute(fileName: String, args: List<String>): Boolean {
        val byteArgs = args.map { it.encodeToByteArray().pin() }
        val convertedArgs = nativeHeap.allocArrayOfPointersTo(byteArgs.map { it.addressOf(0).pointed })

        val result = execvp(fileName, convertedArgs)

        byteArgs.map { it.unpin() }
        return result != -1
    }
}