package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.Monitor
import de.atennert.lcarswm.WindowManagerState
import de.atennert.lcarswm.adjustWindowPositionAndSize
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import de.atennert.lcarswm.windowactions.redrawRootWindow
import kotlinx.cinterop.*
import xlib.*

/**
 * Get RANDR information and update window management accordingly.
 */
fun handleRandrEvent(
    system: SystemApi,
    logger: Logger,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
) {
    logger.logDebug("::handleRandrEvent::handle randr")

    val resources = system.rGetScreenResources(rootWindow)!!
    val primary = system.rGetOutputPrimary(rootWindow)

    val outputs = resources.pointed.outputs

    val sortedMonitors = Array(resources.pointed.noutput)
    { i -> Pair(outputs!![i], system.rGetOutputInfo(resources, outputs[i])) }
        .asSequence()
        .filter { (_, outputObject) ->
            outputObject != null
        }
        .map { (outputId, outputObject) ->
            Triple(outputId, outputObject!!, getOutputName(outputObject))
        }
        .map { (outputId, outputObject, outputName) ->
            Triple(Monitor(outputId, outputName, outputId == primary), outputObject.pointed.crtc, outputObject)
        }
        .onEach { (monitor, c, _) ->
            logger.logInfo("::printOutput::name: ${monitor.name}, id: ${monitor.id} crtc: $c")
        }
        .map { (monitor, crtc, outputObject) ->
            nativeHeap.free(outputObject)
            Pair(monitor, crtc)
        }
        .groupBy { (_, crtc) -> crtc.toInt() != 0 }

    // unused monitors
    sortedMonitors[false]

    val activeMonitors = sortedMonitors[true].orEmpty()
        .map { (monitor, crtcReference) ->
            addMeasurementToMonitor(system, monitor, crtcReference, resources)
        }
        .filter { it.isFullyInitialized }

    val (width, height) = activeMonitors
        .fold(Pair(0, 0)) { (width, height), monitor ->
            var newWidth = width
            var newHeight = height
            if (monitor.x + monitor.width > width) {
                newWidth = monitor.x + monitor.width
            }
            if (monitor.y + monitor.height > height) {
                newHeight = monitor.y + monitor.height
            }
            Pair(newWidth, newHeight)
        }

    system.resizeWindow(rootWindow, width.convert(), height.convert())

    windowManagerState.screenSize = Pair(width, height)
    windowManagerState.updateMonitors(activeMonitors)
    { measurements, window -> adjustWindowPositionAndSize(system, measurements, window) }

    redrawRootWindow(windowManagerState, graphicsContexts, rootWindow, system, image)
}

/**
 * Get the name of the given output.
 */
private fun getOutputName(outputObject: CPointer<XRROutputInfo>): String {
    val name = outputObject.pointed.name
    val nameArray = ByteArray(outputObject.pointed.nameLen) { name!![it] }

    return nameArray.decodeToString()
}

private fun addMeasurementToMonitor(
    system: SystemApi,
    monitor: Monitor,
    crtcReference: RRCrtc,
    resources: CPointer<XRRScreenResources>
): Monitor {
    val crtcInfo = system.rGetCrtcInfo(resources, crtcReference)!!.pointed

    monitor.setMeasurements(crtcInfo.x, crtcInfo.y, crtcInfo.width, crtcInfo.height)

    return monitor
}
