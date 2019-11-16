package de.atennert.lcarswm

class UIDrawingMock : UIDrawing {
    var drawWindowManagerFrameCalls = 0

    override fun drawWindowManagerFrame() {
        drawWindowManagerFrameCalls++
    }
}