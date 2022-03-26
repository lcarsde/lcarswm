package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
import de.atennert.lcarswm.file.Directory
import de.atennert.lcarswm.file.DirectoryFactory
import de.atennert.lcarswm.signal.Signal
import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.*
import platform.linux.mq_attr
import platform.linux.mqd_t
import platform.posix.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunAutostartAppsTest {
    private fun createFakeApi(fileLinesMap: Map<String, List<String>>): FakePosixApi {
        return object : FakePosixApi() {
            override fun getLines(fileName: String): List<String> {
                return fileLinesMap[fileName]
                    ?: super.getLines(fileName)
            }

            override fun access(fileName: String, mode: Int): Int {
                return if (fileLinesMap.containsKey(fileName)) 1 else -1
            }
        }
    }

    private fun createFakeDirectoryFactory(dirFilesMap: Map<String, Set<String>> = emptyMap()): DirectoryFactory {
        return object : DirectoryFactory {
            override fun getDirectory(path: String): Directory? {
                return dirFilesMap[path]
                    ?.let { object : Directory{
                        override fun readFiles(): Set<String> = it
                        override fun close() {}
                    } }
            }
        }
    }

    @Test
    fun `run apps from user desktop file`() {
        val fakeApi = createFakeApi(mapOf(
            "/home/me/config/autostart/runMe.desktop" to listOf("exec=myapp --arg1 -v 42")
        ))

        val fakeFactory = createFakeDirectoryFactory(mapOf(
            "/home/me/config/autostart" to setOf("runMe.desktop")
        ))

        runAutostartApps(fakeApi, fakeFactory)

        assertTrue(fakeApi.areAllFilesClosed())
        assertEquals(mapOf(
            "myapp" to listOf("myapp", "--arg1", "-v", "42")
        ), fakeApi.executions)
    }

    @Test
    fun `run apps from global desktop file`() {
        val fakeApi = createFakeApi(mapOf(
            "/etc/xdg/autostart/runMe.desktop" to listOf("exec=myapp --arg1 -v 42")
        ))

        val fakeFactory = createFakeDirectoryFactory(mapOf(
            "/etc/xdg/autostart" to setOf("runMe.desktop")
        ))

        runAutostartApps(fakeApi, fakeFactory)

        assertTrue(fakeApi.areAllFilesClosed())
        assertEquals(mapOf(
            "myapp" to listOf("myapp", "--arg1", "-v", "42")
        ), fakeApi.executions)
    }

    @Test
    fun `run apps from user autostart file`() {
        val fakeApi = createFakeApi(mapOf(
            "/home/me/config/lcarsde/autostart" to listOf("myapp1", "myapp2 --arg1 -v 42")
        ))

        runAutostartApps(fakeApi, createFakeDirectoryFactory())

        assertTrue(fakeApi.areAllFilesClosed())
        assertEquals(mapOf(
            "myapp1" to listOf("myapp1"),
            "myapp2" to listOf("myapp2", "--arg1", "-v", "42")
        ), fakeApi.executions)
    }

    @Test
    fun `run apps from default autostart file`() {
        val fakeApi = createFakeApi(mapOf(
            "/etc/lcarsde/autostart" to listOf("myapp1", "myapp2 --arg1 -v 42")
        ))

        runAutostartApps(fakeApi, createFakeDirectoryFactory())

        assertTrue(fakeApi.areAllFilesClosed())
        assertEquals(mapOf(
            "myapp1" to listOf("myapp1"),
            "myapp2" to listOf("myapp2", "--arg1", "-v", "42")
        ), fakeApi.executions)
    }

    @Test
    fun `handle unavailable autostart file`() {
        val fakeApi = object : FakePosixApi() {
            override fun fopen(fileName: String, modes: String): CPointer<FILE>? = null

            override fun access(fileName: String, mode: Int): Int = -1
        }

        runAutostartApps(fakeApi, createFakeDirectoryFactory())

        assertTrue(fakeApi.areAllFilesClosed())
        assertTrue(fakeApi.executions.isEmpty())
    }

    open class FakePosixApi : PosixApi {
        override fun getenv(name: String): CPointer<ByteVar>? {
            return when (name) {
                HOME_CONFIG_DIR_PROPERTY -> "/home/me/config"
                else -> error("getenv with unsimulated key: $name")
            }.encodeToByteArray().pin().addressOf(0)
        }

        private val fileMap = mutableMapOf<CPointer<FILE>, MutableList<String>>()

        override fun fopen(fileName: String, modes: String): CPointer<FILE>? {
            val newFilePointer = nativeHeap.alloc<FILE>()
            fileMap[newFilePointer.ptr] = getLines(fileName).toMutableList()
            return newFilePointer.ptr
        }

        open fun getLines(fileName: String): List<String> = emptyList()

        override fun fgets(buffer: CPointer<ByteVar>, bufferSize: Int, file: CPointer<FILE>): CPointer<ByteVar>? {
            val lines = fileMap[file] ?: return null
            if (lines.isEmpty()) {
                return null
            }

            val nextLine = lines.removeAt(0) + "\n"
            val maxUsableCharacters = bufferSize - 1
            if (nextLine.length > maxUsableCharacters) {
                lines.add(0, nextLine.drop(maxUsableCharacters))
            }

            val providedCharacters = nextLine.take(maxUsableCharacters) + "\u0000"
            providedCharacters.encodeToByteArray()
                .forEachIndexed { index, value -> buffer[index] = value }

            return buffer
        }

        override fun fputs(s: String, file: CPointer<FILE>): Int {
            throw NotImplementedError()
        }

        override fun fclose(file: CPointer<FILE>): Int {
            assertTrue(fileMap.contains(file))
            fileMap.remove(file)
            nativeHeap.free(file)
            return 0
        }

        fun areAllFilesClosed(): Boolean {
            return fileMap.isEmpty()
        }

        override fun feof(file: CPointer<FILE>): Int {
            return if (fileMap.contains(file) && fileMap[file]!!.isEmpty()) 1 else 0
        }

        override fun fork(): __pid_t {
            return 0
        }

        override fun setsid(): __pid_t {
            return 0
        }

        override fun setenv(name: String, value: String): Int {
            throw NotImplementedError()
        }

        override fun perror(s: String) {
            throw NotImplementedError()
        }

        override fun exit(status: Int) {
        }

        val executions = mutableMapOf<String, List<String>>()

        override fun execvp(fileName: String, args: List<String>): Int {
            executions[fileName] = args
            return 0
        }

        override fun gettimeofday(): Long {
            throw NotImplementedError()
        }

        override fun usleep(time: UInt) {
            throw NotImplementedError()
        }

        override fun abort() {
            throw NotImplementedError()
        }

        override fun sigFillSet(sigset: CPointer<sigset_t>) {
            throw NotImplementedError()
        }

        override fun sigEmptySet(sigset: CPointer<sigset_t>) {
            throw NotImplementedError()
        }

        override fun sigAction(signal: Signal, newSigAction: CPointer<sigaction>, oldSigAction: CPointer<sigaction>?) {
            throw NotImplementedError()
        }

        override fun sigProcMask(how: Int, newSigset: CPointer<sigset_t>?, oldSigset: CPointer<sigset_t>?) {
            throw NotImplementedError()
        }

        override fun mqOpen(name: String, oFlag: Int, mode: mode_t, attributes: CPointer<mq_attr>): mqd_t {
            throw NotImplementedError()
        }

        override fun mqClose(mq: mqd_t): Int {
            throw NotImplementedError()
        }

        override fun mqSend(mq: mqd_t, msg: String, msgPrio: UInt): Int {
            throw NotImplementedError()
        }

        override fun mqReceive(
            mq: mqd_t,
            msgPtr: CPointer<ByteVar>,
            msgSize: size_t,
            msgPrio: CPointer<UIntVar>?
        ): ssize_t {
            throw NotImplementedError()
        }

        override fun mqUnlink(name: String): Int {
            throw NotImplementedError()
        }

        override fun access(fileName: String, mode: Int): Int {
            throw NotImplementedError()
        }
    }
}