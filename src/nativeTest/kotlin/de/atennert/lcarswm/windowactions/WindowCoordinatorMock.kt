package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.system.FunctionCall

class WindowCoordinatorMock : WindowCoordinator {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun rearrangeActiveWindows() {
        functionCalls.add(FunctionCall("rearrangeActiveWindows"))
    }
}