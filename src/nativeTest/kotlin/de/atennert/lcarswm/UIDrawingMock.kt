package de.atennert.lcarswm

class UIDrawingMock : UIDrawing {
    var drawWindowManagerFrameCallCount = 0

    override fun drawWindowManagerFrame() {
        drawWindowManagerFrameCallCount++
    }
}