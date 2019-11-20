package de.atennert.lcarswm

import de.atennert.lcarswm.system.FunctionCall

class UIDrawingMock : UIDrawing {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun drawWindowManagerFrame() {
        functionCalls.add(FunctionCall("drawWindowManagerFrame"))
    }
}