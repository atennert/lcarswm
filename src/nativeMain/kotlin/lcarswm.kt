import de.atennert.lcarswm.*
import de.atennert.lcarswm.events.EVENT_HANDLERS
import de.atennert.lcarswm.events.handleRandrEvent
import de.atennert.lcarswm.log.FileLogger
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.SystemFacade
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

private var wmDetected = false

// this is a somewhat dirty hack to hand the logger to staticCFunction as error handler
private var staticLogger: Logger? = null

fun main() {
    println("::main::start lcarswm initialization")

    memScoped {
        val system = SystemFacade()
        val logger: Logger = FileLogger(system, LOG_FILE_PATH)
        staticLogger = logger
        val screen = system.defaultScreenOfDisplay()?.pointed ?: error("::main::got no screen")
        val rootWindow = screen.root

        system.setErrorHandler(staticCFunction { _, _ -> wmDetected = true; 0 })

        system.selectInput(rootWindow, SubstructureRedirectMask or SubstructureNotifyMask)
        system.sync(false)

        if (wmDetected) {
            logger.logError("::main::Detected another active window manager")
            return
        }

        system.setErrorHandler(staticCFunction { _, err -> staticLogger!!.logError("::main::error code: ${err?.pointed?.error_code}"); 0 })

        logger.logInfo("::main::Screen size: ${screen.width}/${screen.height}, root: $rootWindow")

        val colorMap = allocateColorMap(system, screen.root_visual, rootWindow)
        val graphicsContexts = getGraphicContexts(system, rootWindow, colorMap.second)

        logger.logInfo("::main::graphics loaded")

        val windowManagerConfig = WindowManagerState { system.internAtom(it, false) }

        logger.logInfo("::main::wm state initialized")

        setupLcarsWindow(system, screen, windowManagerConfig)
        windowManagerConfig.setActiveWindowListener { activeWindow ->
            if (activeWindow != null) {
                system.setInputFocus(activeWindow.id, RevertToNone, CurrentTime.convert())
            } else {
                system.setInputFocus(rootWindow, RevertToPointerRoot, CurrentTime.convert())
            }
        }

        logger.logInfo("::main::wm window initialized: $rootWindow")

        val logoImage = allocArrayOfPointersTo(alloc<XImage>())

        system.readXpmFileToImage("/usr/share/pixmaps/lcarswm.xpm", logoImage)

        logger.logInfo("::main::logo loaded")

        val randrBase = setupRandr(system, windowManagerConfig, logoImage[0]!!, rootWindow, graphicsContexts)

        logger.logInfo("::main::set up randr")

        setupScreen(system, rootWindow, windowManagerConfig)

        logger.logInfo("::main::loaded window tree")

        eventLoop(system, windowManagerConfig, randrBase, logoImage[0]!!, rootWindow, graphicsContexts)

        cleanupColorMap(system, colorMap)

        system.closeDisplay()

        staticLogger = null
        logger.logInfo("::main::lcarswm stopped")
        logger.close()
    }
}

fun setupScreen(system: SystemApi, rootWindow: Window, windowManagerConfig: WindowManagerState) {
    system.grabServer()

    val returnedWindows = ULongArray(1)
    val returnedParent = ULongArray(1)
    val topLevelWindows = nativeHeap.allocPointerTo<ULongVarOf<Window>>()
    val topLevelWindowCount = UIntArray(1)

    system.queryTree(rootWindow, returnedWindows.toCValues(), returnedParent.toCValues(),
        topLevelWindows.ptr,
        topLevelWindowCount.toCValues())

    ULongArray(topLevelWindowCount[0].toInt()) {topLevelWindows.value!![it]}
        .filter { childId -> childId != rootWindow }
        .forEach { childId ->
            addWindow(system, windowManagerConfig, rootWindow, childId, true)
        }

    nativeHeap.free(topLevelWindows)
    system.ungrabServer()
}

/**
 * @return RANDR base value
 */
private fun setupRandr(
    system: SystemApi,
    windowManagerState: WindowManagerState,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
): Int {
    val eventBase = IntArray(1).pin()
    val errorBase = IntArray(1).pin()

    if (system.rQueryExtension(eventBase.addressOf(0), errorBase.addressOf(0)) == X_FALSE) {
        println("::setupRandr::no RANDR extension")
        return NO_RANDR_BASE
    }

    handleRandrEvent(system, windowManagerState, image, rootWindow, graphicsContexts)

    system.rSelectInput(rootWindow,
        (RRScreenChangeNotifyMask or
                RROutputChangeNotifyMask or
                RRCrtcChangeNotifyMask or
                RROutputPropertyNotifyMask).convert() )

    println("::setupRandr::RANDR base: ${eventBase.get()[0]}, error base: ${errorBase.get()[0]}")

    return eventBase.get()[0]
}

private fun eventLoop(
    system: SystemApi,
    windowManagerState: WindowManagerState,
    randrBase: Int,
    image: CPointer<XImage>,
    rootWindow: Window,
    graphicsContexts: List<GC>
) {
    val randrEventValue = randrBase + RRScreenChangeNotify

    while (true) {
        val xEvent = nativeHeap.alloc<XEvent>()
        system.nextEvent(xEvent.ptr)
        val eventValue = xEvent.type

        if (eventValue == randrEventValue) {
            println("::eventLoop::received randr event")
            handleRandrEvent(system, windowManagerState, image, rootWindow, graphicsContexts)
            nativeHeap.free(xEvent)
            continue
        }

        if (EVENT_HANDLERS.containsKey(xEvent.type)) {
            val stop = EVENT_HANDLERS[xEvent.type]!!.invoke(system, windowManagerState, xEvent, image, rootWindow, graphicsContexts)
            if (stop) {
                break
            }
        } else {
            println("::eventLoop::unhandled event: ${xEvent.type}")
        }

        nativeHeap.free(xEvent)
    }
}
