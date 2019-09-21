package de.atennert.lcarswm.log

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.*
import platform.posix.FILE
import kotlin.test.Test
import kotlin.test.assertEquals

class FileLoggerTest {
    @Test
    fun `open and close file logger`() {
        val filePointer = nativeHeap.alloc<FILE>().ptr
        val posixApi = PosixApiMock(filePointer)
        val filePath = "/this/is/my/logfile.log"

        val fileLogger = FileLogger(posixApi, filePath)
        assertEquals(filePath, posixApi.fopenFileName, "handed in file path not used")
        assertEquals("w", posixApi.fopenMode, "not using log file for write only")

        fileLogger.close()
        assertEquals(filePointer, posixApi.fcloseFile, "closed other file then opened")
    }

    @Test
    fun `write to file`() {
        val filePointer = nativeHeap.alloc<FILE>().ptr
        val posixApi= PosixApiMock(filePointer)
        val text = "this is my text"

        val fileLogger = FileLogger(posixApi, "")

        fileLogger.printLn(text)
        assertEquals("$text\n", posixApi.fputsText, "the written text doesn't fit (maybe missing \\n?)")
        assertEquals(filePointer, posixApi.fputsFile, "doesn't write output to given file")
    }

    private class PosixApiMock(val filePointer: CPointer<FILE>) : SystemFacadeMock() {
        var fopenFileName: String? = null
        var fopenMode: String? = null

        var fcloseFile: CPointer<FILE>? = null

        var fputsText: String? = null
        var fputsFile: CPointer<FILE>? = null

        override fun fopen(fileName: String, modes: String): CPointer<FILE>? {
            this.fopenFileName = fileName
            this.fopenMode = modes
            return filePointer
        }

        override fun fclose(file: CPointer<FILE>): Int {
            this.fcloseFile = file
            return 0
        }

        override fun fputs(s: String, file: CPointer<FILE>): Int {
            this.fputsText = s
            this.fputsFile = file
            return 0
        }
    }
}