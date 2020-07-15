import de.atennert.lcarswm.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.runtime.AppMenuResources
import de.atennert.lcarswm.runtime.RuntimeResources
import de.atennert.lcarswm.runtime.XEventResources
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import xlib.*

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
var staticLogger: Logger? = null

val exitState = atomic<Int?>(null)

const val ROOT_WINDOW_MASK = SubstructureRedirectMask or StructureNotifyMask or PropertyChangeMask or
        FocusChangeMask or KeyPressMask or KeyReleaseMask

const val XRANDR_MASK = RRScreenChangeNotifyMask or RROutputChangeNotifyMask or
        RRCrtcChangeNotifyMask or RROutputPropertyNotifyMask

// the main method apparently must not be inside of a package so it can be compiled with Kotlin/Native
fun main() = runBlocking {
    val system = SystemFacade()
    val cacheDirPath = system.getenv(HOME_CACHE_DIR_PROPERTY)?.toKString() ?: error("::main::cache dir not set")
    val logger = FileLogger(system, cacheDirPath + LOG_FILE_PATH)

    runWindowManager(system, logger)

    system.exit(exitState.value ?: -1)
}

suspend fun runWindowManager(system: SystemApi, logger: Logger) = coroutineScope {
    logger.logInfo("::runWindowManager::start lcarswm initialization")
    exitState.value = null
    staticLogger = logger

    val runtimeResources = startup(system, logger)

    runtimeResources?.let {
        runLcarswmTools(system)

        runEventLoops(logger, it)
    }

    shutdown(system)
}

private suspend fun runEventLoops(
    logger: Logger,
    runtimeResources: RuntimeResources
) = coroutineScope {

    val appMenuJob = runAppMenuLoop(logger, runtimeResources.appMenu)
    val xEventJob = runXEventLoop(logger, runtimeResources.xEvent)

    // When the X event loop goes down, so does everything else
    xEventJob.invokeOnCompletion {
        appMenuJob.cancel()
    }

    appMenuJob.join()
    logger.logDebug("::eventLoop::finished event loops")
}

private fun CoroutineScope.runXEventLoop(
    logger: Logger,
    xEventResources: XEventResources
): Job {
    val eventBuffer = xEventResources.eventBuffer
    val eventTime = xEventResources.eventTime
    val eventDistributor = xEventResources.eventHandler

    val xEventJob = launch {
        logger.logDebug("::runXEventLoop::running X event loop")
        while (exitState.value == null) {
            val xEvent = eventBuffer.getNextEvent(false)?.pointed

            if (xEvent == null) {
                delay(50)
                continue
            }

            eventTime.setTimeFromEvent(xEvent.ptr)

            if (eventDistributor.handleEvent(xEvent)) {
                exitState.value = 0
            }

            eventTime.unsetEventTime()

            nativeHeap.free(xEvent)
        }
    }
    xEventJob.invokeOnCompletion { logger.logDebug("::runXEventLoop::X event job completed") }
    return xEventJob
}

private fun CoroutineScope.runAppMenuLoop(
    logger: Logger,
    appMenuResources: AppMenuResources
): Job {
    val appMenuMessageQueue = appMenuResources.messageQueue
    val appMenuMessageHandler = appMenuResources.messageHandler

    val appMenuJob = launch {
        logger.logDebug("::runAppMenuLoop::running app menu job")
        while (true) {
            appMenuMessageQueue.receiveMessage()?.let {
                appMenuMessageHandler.handleMessage(it)
            }
            delay(100)
        }
    }
    appMenuJob.invokeOnCompletion { logger.logDebug("::runAppMenuLoop::app menu job completed") }
    return appMenuJob
}
