package de.atennert.lcarswm

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
    private val settingsFilePath = "$configPath/$LCARS_WM_DIR/settings.xml"

    private var keyConfiguration: List<KeyBinding>? = null
    private var generalSettings: Map<String, String>? = null

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

        if (readUbyteString(rootElement.name) != "lcarswm") {
            logger.logWarning("SettingsReader::readSettingsFromDocument::didn't find root tag: ${readUbyteString(rootElement.name)}")
            return false
        }

        var node = rootElement.children?.get(0)
        while (node != null) {
            logger.logDebug("read: ${readUbyteString(node.name)}")
            val successful = when (readUbyteString(node.name)) {
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
        val keyConfigXml = mutableListOf<KeyBinding>()

        while (bindingNode != null) {
            if (bindingNode.type != XML_ELEMENT_NODE) {
                bindingNode = bindingNode.next?.pointed
                continue
            }

            logger.logDebug("read-config: ${readUbyteString(bindingNode.name)}")
            val keyBinding = getBinding(bindingNode) ?: return false
            logger.logDebug("read-config: ${keyBinding.keys}->${keyBinding.command}")
            keyConfigXml.add(keyBinding)

            bindingNode = bindingNode.next?.pointed
        }
        return true
    }

    private fun getBinding(bindingNode: _xmlNode): KeyBinding? {
        val keysNode = getNodeForName(bindingNode, "keys") ?: return null

        val execNode = getNodeForName(bindingNode, "exec")

        val keys = readUbyteString(keysNode.children?.get(0)?.content)
        if (keys.isEmpty()) return null

        if (execNode != null) { // check if it's a command execution
            val exec = readUbyteString(execNode.children?.get(0)?.content)
            if (exec.isEmpty()) return null

            return KeyExecution(keys, exec)
        } else { // check if it's a window manager action
            val actionNode = getNodeForName(bindingNode, "action") ?: return null
            val action = readUbyteString(actionNode.children?.get(0)?.content)
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

            nodeName = readUbyteString(generalNode.name)
            textContent = readUbyteString(generalNode.children?.get(0)?.content)

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

    private fun readUbyteString(ubyteStringPointer: CPointer<xmlCharVar>?): String {
        if (ubyteStringPointer == null) {
            return ""
        }

        val byteString = mutableListOf<Byte>()
        var i = 0
        while (true) {
            val value = ubyteStringPointer[i]
            if (value.convert<Int>() == 0) {
                break
            }

            byteString.add(value.convert())
            i++
        }
        return byteString.toByteArray().toKString()
    }

    private fun loadDefaultSettings() {
        keyConfiguration = listOf(
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