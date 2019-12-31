package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.*

/**
 * Run a given program.
 * @param programPath the path to the program
 */
fun runProgram(posixApi: PosixApi, programPath: String, args: List<String>): Boolean {
    when (posixApi.fork()) {
        -1 -> return false
        0 -> {
            if (posixApi.setsid() == -1) {
                posixApi.perror("setsid failed")
                posixApi.exit(1)
            }

            programPath.encodeToByteArray().pin()
            val byteArgs = args.map { it.encodeToByteArray().pin().addressOf(0).pointed }
            val argv = nativeHeap.allocArrayOfPointersTo(byteArgs)

            if (posixApi.execvp(programPath, argv) == -1) {
                posixApi.perror("execvp failed")
                posixApi.exit(1)
            }

            posixApi.exit(0)
        }
    }
    return true
}