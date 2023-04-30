package de.atennert.lcarswm.window

import de.atennert.lcarswm.lifecycle.closeWith
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.wrapXRestackWindows
import de.atennert.rx.NextObserver
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import xlib.Display
import xlib.Window

class WindowStack(
    private val logger: Logger,
    private val display: CPointer<Display>?,
    windowList: WindowList,
    focusHandler: WindowFocusHandler
) {
    private val stack = mutableListOf<ManagedWmWindow<Window>>()
    private var activeWindow: Window? = null

    private val comparator = Comparator<ManagedWmWindow<Window>> { a, b ->
        val aValue = getWindowValue(a)
        val bValue = getWindowValue(b)

        bValue - aValue
    }

    init {
        windowList.windowEventObs
            .subscribe(NextObserver {
                if (it is WindowAddedEvent) {
                    stack.add(0, it.window)
                } else if (it is WindowRemovedEvent) {
                    stack.remove(it.window)
                }
                stack.sortWith(comparator)
                logger.logDebug("STACK::add/remove::$stack")
                triggerWmRestack()
            })
            .closeWith { this.unsubscribe() }

        focusHandler.windowFocusEventObs
            .subscribe(NextObserver { (newWindow) ->
                activeWindow = newWindow
                logger.logDebug("STACK::focus::$activeWindow")
                if (newWindow == null) {
                    return@NextObserver
                }
                stack.find { it.id == newWindow }
                    ?.let {
                        stack.remove(it)
                        stack.add(0, it)
                        logger.logDebug("STACK::focus::$stack")
                        stack.sortWith(comparator)
                        triggerWmRestack()
                    }
            })
            .closeWith { this.unsubscribe() }
    }

    private fun getWindowValue(a: ManagedWmWindow<Window>): Int {
        var value = if (a is PosixTransientWindow && a.transientFor == null) 100 else 0 // transient for root
        value += if (a.id == activeWindow) 1 else 0
        return value
    }

    private fun triggerWmRestack() {
        if (stack.isEmpty()) {
            return
        }
        stack.map { it.frame }
            .toULongArray()
            .usePinned {
                logger.logDebug("STACK::send::${it.get()}")
                wrapXRestackWindows(display, it.addressOf(0), it.get().size)
            }
    }
}