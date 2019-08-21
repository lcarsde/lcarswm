package de.atennert.lcarswm

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Run a given program.
 * @param programPath the path to the program
 */
fun runProgram(programPath: String, args: List<String>): Boolean {
    when (fork()) {
        -1 -> return false
        0 -> {
            if (setsid() == -1) {
                perror("setsid failed")
                exit(1)
            }

            programPath.encodeToByteArray().pin()
            val argv = args.map { it.encodeToByteArray().pin().addressOf(0) }

            if (execvp(programPath, argv.toCValues()) == -1) {
                perror("execvp failed")
                exit(1)
            }

            exit(0)
        }
    }
    return true
}