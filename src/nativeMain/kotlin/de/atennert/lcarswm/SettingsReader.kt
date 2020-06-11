package de.atennert.lcarswm

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.XML_ELEMENT_NODE
import xlib._xmlNode
import xlib.xmlCharVar
import xlib.xmlDoc

class SettingsReader(private val logger: Logger, private val systemApi: SystemApi) {
    private val settingsFilePath = ""

    private var keyConfiguration: List<KeyBinding>? = null
    private var generalSettings: Map<String, String>? = null

    init {
        if (!doUserSettingsExist()) {
            addDefaultSettings()
        }

        loadSettings()
    }

    private fun doUserSettingsExist(): Boolean {
        val filePointer = systemApi.fopen(settingsFilePath, "r") ?: return false
        systemApi.fclose(filePointer)
        return true
    }

    private fun addDefaultSettings(): Boolean {
        return SettingsWriter.writeInitialSettings(systemApi, settingsFilePath)
    }

    private fun loadSettings() {
        val document = systemApi.readXmlFile(settingsFilePath)
        if (document != null) {
            logger.logDebug("SettingsReader::loadSettings::loading settings from XML")
            if (!readSettingsFromDocument(document)) {
                logger.logDebug("SettingsReader::loadSettings::loading internal default settings")
                loadDefaultSettings()
            }
            systemApi.freeXmlDoc(document)
        } else {
            logger.logDebug("SettingsReader::loadSettings::loading internal default settings")
            loadDefaultSettings()
        }
    }

    private fun readSettingsFromDocument(document: CPointer<xmlDoc>): Boolean {
        val rootElement = systemApi.getXmlRootElement(document)?.pointed ?: return false

        if (readUbyteString(rootElement.name) != "lcarswm") {
            return false
        }

        var node = rootElement.children?.get(0)
        while (node != null) {
            val successful = when (readUbyteString(node.name)) {
                "key-config" -> readKeyConfig(node)
                "general" -> readGeneralConfig(node)
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
                logger.logDebug("SettingsReader::readKeyConfig::node type: ${bindingNode.type}")
                bindingNode = node.next?.pointed
                continue
            }

            val keyBinding = getBinding(bindingNode) ?: return false
            keyConfigXml.add(keyBinding)

            bindingNode = node.next?.pointed
        }
        return true
    }

    private fun getBinding(bindingNode: _xmlNode): KeyBinding? {
        val node1 = bindingNode.children?.get(0) ?: return null
        val node2 = bindingNode.children?.get(1) ?: return null

        val node1Name = readUbyteString(node1.name)
        val node2Name = readUbyteString(node2.name)

        val keysNode = getNodeForName("keys", node1Name, node1, node2Name, node2) ?: return null
        val execNode = getNodeForName("exec", node1Name, node1, node2Name, node2)

        val keys = readUbyteString(keysNode.children?.get(0)?.content)
        if (keys.isEmpty()) return null

        if (execNode != null) {
            val exec = readUbyteString(execNode.children?.get(0)?.content)
            if (exec.isEmpty()) return null

            return KeyExecution(keys, exec)
        } else {
            val actionNode = getNodeForName("action", node1Name, node1, node2Name, node2) ?: return null
            val action = readUbyteString(actionNode.children?.get(0)?.content)
            if (action.isEmpty()) return null

            return KeyAction(keys, action)
        }
    }

    private fun getNodeForName(
        targetName: String,
        node1Name: String,
        node1: _xmlNode,
        node2Name: String,
        node2: _xmlNode
    ): _xmlNode? {
        return when (targetName) {
            node1Name -> node1
            node2Name -> node2
            else -> null
        }
    }

    private fun readGeneralConfig(node: _xmlNode): Boolean {
        var generalNode = node.children?.get(0)
        var nodeName: String
        var textContent: String
        val generalSettingsXml = mutableMapOf<String, String>()

        while (generalNode != null) {
            if (generalNode.type != XML_ELEMENT_NODE) {
                logger.logDebug("SettingsReader::readGeneralConfig::node type: ${generalNode.type}")
                generalNode = node.next?.pointed
                continue
            }

            nodeName = readUbyteString(generalNode.name)
            textContent = readUbyteString(generalNode.children?.get(0)?.content)

            if (nodeName.isNotEmpty() && textContent.isNotEmpty()) {
                generalSettingsXml[nodeName] = textContent
            } else {
                return false
            }

            generalNode = node.next?.pointed
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