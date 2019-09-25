package de.atennert.lcarswm

import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 *
 */
fun addWindow(system: SystemApi, logger: Logger, windowManagerState: WindowManagerStateHandler, rootWindow: Window, windowId: Window, isSetup: Boolean) {
    val windowAttributes = nativeHeap.alloc<XWindowAttributes>()
    system.getWindowAttributes(windowId, windowAttributes.ptr)

    if (windowAttributes.override_redirect != 0 || (isSetup &&
            windowAttributes.map_state != IsViewable)) {
        logger.logInfo("::addWindow::skipping window $windowId")
        nativeHeap.free(windowAttributes)
        return
    }

    val window = WindowContainer(windowId)
    val windowMonitor = windowManagerState.initialMonitor

    val measurements = windowMonitor.getCurrentWindowMeasurements(windowManagerState.getScreenModeForMonitor(windowMonitor))

    window.frame = system.createSimpleWindow(rootWindow, measurements)

    system.selectInput(window.frame, SubstructureRedirectMask or SubstructureNotifyMask)

    system.addToSaveSet(windowId)

    system.reparentWindow(windowId, window.frame, 0, 0)

    system.resizeWindow(window.id, measurements[2].convert(), measurements[3].convert())

    system.mapWindow(window.frame)

    system.mapWindow(window.id)

    windowManagerState.addWindow(window, windowMonitor)

    nativeHeap.free(windowAttributes)
}