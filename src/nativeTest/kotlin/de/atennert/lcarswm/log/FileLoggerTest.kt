package de.atennert.lcarswm.log

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.FILE
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileLoggerTest {
    private val filePath = "/this/is/my/logfile.log"
    private lateinit var filePointer: CPointer<FILE>
    private lateinit var posixApi: PosixApiMock

    @BeforeTest
    fun setup() {
        this.filePointer = nativeHeap.alloc<FILE>().ptr
        this.posixApi = PosixApiMock(filePointer)
    }

    @Test
    fun `open and close file logger`() {
        val fileLogger = FileLogger(this.posixApi, this.filePath)
        assertEquals(this.filePath, this.posixApi.fopenFileName, "handed in file path not used")
        assertEquals("w", this.posixApi.fopenMode, "not using log file for write only")

        fileLogger.close()
        assertEquals(this.filePointer, this.posixApi.fcloseFile, "closed other file then opened")
    }

    @Test
    fun `log debug info to file`() {
        val text = "this is my text"

        val fileLogger = FileLogger(this.posixApi, this.filePath)

        fileLogger.logDebug(text)
        assertEquals("DEBUG: $text\n", this.posixApi.fputsText, "the written text doesn't fit (maybe missing \\n?)")
        assertEquals(this.filePointer, this.posixApi.fputsFile, "doesn't write output to given file")
    }

    @Test
    fun `log info to file`() {
        val text = "this is my text"

        val fileLogger = FileLogger(this.posixApi, this.filePath)

        fileLogger.logInfo(text)
        assertEquals(" INFO: $text\n", this.posixApi.fputsText, "the written text doesn't fit (maybe missing \\n?)")
        assertEquals(this.filePointer, this.posixApi.fputsFile, "doesn't write output to given file")
    }

    @Test
    fun `log warning to file`() {
        val text = "this is my warning"

        val fileLogger = FileLogger(this.posixApi, this.filePath)

        fileLogger.logWarning(text)
        assertEquals(" WARN: $text\n", this.posixApi.fputsText, "the written text doesn't fit (maybe missing \\n?)")
        assertEquals(this.filePointer, this.posixApi.fputsFile, "doesn't write output to given file")
    }

    @Test
    fun `log error to file`() {
        val text = "this is my error"

        val fileLogger = FileLogger(this.posixApi, this.filePath)

        fileLogger.logError(text)
        assertEquals("ERROR: $text\n", this.posixApi.fputsText, "the written text doesn't fit (maybe missing \\n?)")
        assertEquals(this.filePointer, this.posixApi.fputsFile, "doesn't write output to given file")
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
            return this.filePointer
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