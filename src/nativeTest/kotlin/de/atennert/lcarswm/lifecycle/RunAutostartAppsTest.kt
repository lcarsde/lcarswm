package de.atennert.lcarswm.lifecycle

import de.atennert.lcarswm.HOME_CONFIG_DIR_PROPERTY
import de.atennert.lcarswm.command.Commander
import de.atennert.lcarswm.environment.Environment
import de.atennert.lcarswm.file.Directory
import de.atennert.lcarswm.file.DirectoryFactory
import de.atennert.lcarswm.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class RunAutostartAppsTest {
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

    private class FakeCommander : Commander() {
        val calls = mutableListOf<List<String>>()

        override fun run(command: List<String>): Boolean {
            calls.add(command)
            return true
        }
    }

    private class FakeFiles(val files: List<String>, val lines: Map<String, List<String>>) : Files {
        override fun exists(path: String): Boolean {
            return files.contains(path)
        }

        override fun readLines(path: String, consumer: (String) -> Unit) {
            lines[path]?.forEach(consumer)
        }
    }

    private class FakeEnvironment : Environment {
        override fun get(name: String): String? {
            return when(name) {
                HOME_CONFIG_DIR_PROPERTY -> "/home/me/config"
                else -> null
            }
        }

        override fun set(name: String, value: String): Boolean = false
    }

    @Test
    fun `run apps from user desktop file`() {
        val lines = mapOf(
            "/home/me/config/autostart/runMe.desktop" to listOf("exec=myapp --arg1 -v 42")
        )

        val fakeFactory = createFakeDirectoryFactory(mapOf(
            "/home/me/config/autostart" to setOf("runMe.desktop")
        ))

        val commander = FakeCommander()

        runAutostartApps(FakeEnvironment(), fakeFactory, commander, FakeFiles(listOf(), lines))

        assertContains(commander.calls, listOf("myapp", "--arg1", "-v", "42"))
    }

    @Test
    fun `run apps from global desktop file`() {
        val lines = mapOf(
            "/etc/xdg/autostart/runMe.desktop" to listOf("exec=myapp --arg1 -v 42")
        )

        val fakeFactory = createFakeDirectoryFactory(mapOf(
            "/etc/xdg/autostart" to setOf("runMe.desktop")
        ))

        val commander = FakeCommander()

        runAutostartApps(FakeEnvironment(), fakeFactory, commander, FakeFiles(listOf(), lines))

        assertContains(commander.calls, listOf("myapp", "--arg1", "-v", "42"))
    }

    @Test
    fun `run apps from user autostart file`() {
        val lines = mapOf(
            "/home/me/config/lcarsde/autostart" to listOf("myapp1", "myapp2 --arg1 -v 42")
        )

        val commander = FakeCommander()

        runAutostartApps(FakeEnvironment(), createFakeDirectoryFactory(), commander, FakeFiles(listOf("/home/me/config/lcarsde/autostart"), lines))

        assertContains(commander.calls, listOf("myapp1"))
        assertContains(commander.calls, listOf("myapp2", "--arg1", "-v", "42"))
    }

    @Test
    fun `run apps from default autostart file`() {
        val lines = mapOf(
            "/etc/lcarsde/autostart" to listOf("myapp1", "myapp2 --arg1 -v 42")
        )

        val commander = FakeCommander()

        runAutostartApps(FakeEnvironment(), createFakeDirectoryFactory(), commander, FakeFiles(listOf("/etc/lcarsde/autostart"), lines))

        assertContains(commander.calls, listOf("myapp1"))
        assertContains(commander.calls, listOf("myapp2", "--arg1", "-v", "42"))
    }

    @Test
    fun `handle unavailable autostart file`() {
        val commander = FakeCommander()

        runAutostartApps(FakeEnvironment(), createFakeDirectoryFactory(), commander, FakeFiles(listOf(), mapOf()))

        assertTrue(commander.calls.isEmpty())
    }
}