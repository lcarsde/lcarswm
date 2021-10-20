import de.atennert.lcarswm.HOME_CACHE_DIR_PROPERTY
import de.atennert.lcarswm.LOG_FILE_PATH
import de.atennert.lcarswm.lifecycle.*
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.toKString
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
var staticLogger: Logger? = null

/**
 * Some shitty global state variable that is used in different places to handle exiting the main event loop and hold
 * the exit value for the application
 */
val exitState = atomic<Int?>(null)

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

    try {
        runtimeResources?.let {
            runAutostartApps(system)

            runEventLoops(logger, it)
        }
    } catch (e: Throwable) {
        logger.logError("::runWindowManager::error during startup or runtime", e)
    }

    shutdown(system)
}
