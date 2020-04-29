package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.atom.AtomLibrary
import de.atennert.lcarswm.atom.Atoms
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.Window
import xlib.XTextProperty

class WindowNameReader(private val system: SystemApi, private val atomLibrary: AtomLibrary) {
    fun getWindowName(windowId: Window): String {
        val textProperty = nativeHeap.alloc<XTextProperty>()
        var result = system.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.NET_WM_NAME])

        if (result != 0 || textProperty.encoding != atomLibrary[Atoms.UTF_STRING]) {
            result = system.getTextProperty(windowId, textProperty.ptr, atomLibrary[Atoms.WM_NAME])
            if (result == 0) {
                return "UNKNOWN"
            }
        }

        val name = when (textProperty.encoding) {
            atomLibrary[Atoms.COMPOUND_TEXT] -> {
                val localeString = getByteArrayListFromCompound(textProperty).toKString()
                val readBytes = ULongArray(1).pin()
                val utfBytes = system.localeToUtf8(localeString, (-1).convert(), readBytes.addressOf(0))
                    ?: system.localeToUtf8(localeString, readBytes.get()[0].convert(), null)
                    ?: return "unknown"
                ByteArray(readBytes.get()[0].convert()) { utfBytes[it] }.toKString()
            }
            atomLibrary[Atoms.UTF_STRING] -> {
                getByteArray(textProperty).toKString()
            }
            atomLibrary[Atoms.STRING] -> {
                val latinString = getByteArray(textProperty).takeWhile { c ->
                    // filter forbidden control characters
                    c.toInt() == 9 ||
                            c.toInt() == 10 ||
                            c.toInt() in 32..126 ||
                            c.toInt() > 160
                }.toByteArray()
                    .toKString()
                    .trim()
                val readBytes = ULongArray(1).pin()
                val utfBytes = system.convertLatinToUtf8(latinString, (-1).convert(), readBytes.addressOf(0))
                    ?: system.convertLatinToUtf8(latinString, readBytes.get()[0].convert(), null)
                    ?: return "unknown"
                ByteArray(readBytes.get()[0].convert()) { utfBytes[it] }.toKString()
            }
            else -> ""
        }
        system.free(textProperty.value)
        nativeHeap.free(textProperty)
        return if (name.isEmpty()) {
            "-"
        } else {
            name.toUpperCase()
        }
    }

    private fun getByteArrayListFromCompound(textProperty: XTextProperty): ByteArray {
        val resultList = nativeHeap.allocPointerTo<CPointerVar<ByteVar>>()
        val listCount = IntArray(1).pin()
        system.xmbTextPropertyToTextList(textProperty.ptr, resultList.ptr, listCount.addressOf(0))

        val byteList = mutableListOf<Byte>()
        var index = 0
        var value = resultList.value?.get(0)?.get(0)
        while (value != null && value.toInt() != 0) {
            byteList.add(value)
            index++
            value = resultList.value?.get(0)?.get(index)
        }
        byteList.add(0)
        nativeHeap.free(resultList)
        return byteList.toByteArray()
    }

    private fun getByteArray(textProperty: XTextProperty): ByteArray {
        return UByteArray(textProperty.nitems.convert()) { textProperty.value?.get(it)!! }
            .fold(mutableListOf<Byte>()) { list, ub ->
                list.add(ub.convert())
                if (ub.convert<Int>() == 0) {
                    return@fold list
                }
                list
            }
            .toByteArray()
    }
}