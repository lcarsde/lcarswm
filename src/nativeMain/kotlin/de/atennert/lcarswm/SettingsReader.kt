package de.atennert.lcarswm

import de.atennert.lcarswm.conversion.toKString
import de.atennert.lcarswm.conversion.toUByteArray
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import platform.posix.F_OK
import xlib.*

class SettingsReader(
    private val logger: Logger,
    private val systemApi: SystemApi,
    configPath: String
) {
    private val settingsFilePath = "$configPath$SETTINGS_FILE"

    var keyConfiguration: Set<KeyBinding> = emptySet()
    private set

    var generalSettings: Map<String, String> = emptyMap()
    private set

    init {
        if (!doUserSettingsExist()) {
            addDefaultSettings()
        }

        loadSettings()
    }

    private fun doUserSettingsExist(): Boolean {
        return systemApi.access(settingsFilePath, F_OK) != -1
    }

    private fun addDefaultSettings(): Boolean {
        logger.logDebug("SettingsReader::addDefaultSettings::write initial settings")
        return SettingsWriter.writeInitialSettings(systemApi, settingsFilePath)
    }

    private fun loadSettings() {
        val document = systemApi.readXmlFile(settingsFilePath)
        if (document != null) {
            logger.logDebug("SettingsReader::loadSettings::loading settings from XML")
            if (!readSettingsFromDocument(document)) {
                logger.logInfo("SettingsReader::loadSettings::loading internal default settings")
                loadDefaultSettings()
            }
            systemApi.freeXmlDoc(document)
        } else {
            logger.logInfo("SettingsReader::loadSettings::loading internal default settings")
            loadDefaultSettings()
        }
    }

    private fun readSettingsFromDocument(document: CPointer<xmlDoc>): Boolean {
        val rootElement = systemApi.getXmlRootElement(document)?.pointed ?: return false

        if (rootElement.name.toKString() != "lcarswm") {
            logger.logWarning("SettingsReader::readSettingsFromDocument::didn't find root tag: ${rootElement.name.toKString()}")
            return false
        }

        var node = rootElement.children?.get(0)
        while (node != null) {
            val successful = when (node.name.toKString()) {
                "key-config" -> readKeyConfig(node)
                "general" -> readGeneralConfig(node)
                "text" -> true // ignore text
                else -> false
            }
            if (!successful) {
                return false
            }

            node = node.next?.pointed
        }
        return true
    }

    private fun readKeyConfig(node: _xmlNode): Boolean {
        var bindingNode = node.children?.get(0)
        val keyConfigXml = mutableSetOf<KeyBinding>()

        while (bindingNode != null) {
            if (bindingNode.type != XML_ELEMENT_NODE) {
                bindingNode = bindingNode.next?.pointed
                continue
            }

            val keyBinding = getBinding(bindingNode) ?: return false
            logger.logDebug("read-config: ${keyBinding.keys}->${keyBinding.command}")
            keyConfigXml.add(keyBinding)

            bindingNode = bindingNode.next?.pointed
        }
        keyConfiguration = keyConfigXml
        return true
    }

    private fun getBinding(bindingNode: _xmlNode): KeyBinding? {
        val keysNode = getNodeForName(bindingNode, "keys") ?: return null

        val execNode = getNodeForName(bindingNode, "exec")

        val keys = keysNode.children?.get(0)?.content.toKString()
        if (keys.isEmpty()) return null

        if (execNode != null) { // check if it's a command execution
            val exec = execNode.children?.get(0)?.content.toKString()
            if (exec.isEmpty()) return null

            return KeyExecution(keys, exec)
        } else { // check if it's a window manager action
            val actionNode = getNodeForName(bindingNode, "action") ?: return null
            val action = actionNode.children?.get(0)?.content.toKString()
            if (action.isEmpty()) return null

            return KeyAction(keys, action)
        }
    }

    private fun getNodeForName(parentNode: _xmlNode, nodeName: String): _xmlNode? {
        val nodeNamePtr = nodeName.toUByteArray().toCValues()
        var targetNode = parentNode.children?.get(0)

        while (targetNode != null) {
            if (xmlStrcmp(targetNode.name, nodeNamePtr) == 0) {
                return targetNode
            }
            targetNode = targetNode.next?.pointed
        }
        return null
    }

    private fun readGeneralConfig(node: _xmlNode): Boolean {
        var generalNode = node.children?.get(0)
        var nodeName: String
        var textContent: String
        val generalSettingsXml = mutableMapOf<String, String>()

        while (generalNode != null) {
            if (generalNode.type != XML_ELEMENT_NODE) {
                generalNode = generalNode.next?.pointed
                continue
            }

            nodeName = generalNode.name.toKString()
            textContent = generalNode.children?.get(0)?.content.toKString()

            if (nodeName.isNotEmpty() && textContent.isNotEmpty()) {
                generalSettingsXml[nodeName] = textContent
            } else {
                return false
            }

            generalNode = generalNode.next?.pointed
        }
        generalSettings = generalSettingsXml
        return true
    }

    private fun loadDefaultSettings() {
        keyConfiguration = setOf(
            KeyExecution("XF86AudioMute", "amixer set Master toggle"),
            KeyExecution("XF86AudioRaiseVolume", "amixer set Master 3%+"),
            KeyExecution("XF86AudioLowerVolume", "amixer set Master 3%-"),
            KeyAction("Alt+Tab", "window-toggle-forward"),
            KeyAction("Alt+Up", "window-move-up"),
            KeyAction("Alt+Down", "window-move-down"),
            KeyAction("Alt+F4", "window-close"),
            KeyAction("Lin+M", "screen-mode-toggle"),
            KeyAction("Lin+Q", "lcarswm-quit")
        )

        generalSettings = mapOf(
            Pair("title", "LCARS"),
            Pair("font", "Ubuntu Condensed")
        )
    }
}