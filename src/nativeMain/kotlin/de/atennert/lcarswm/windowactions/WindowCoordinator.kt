package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.monitor.Monitor
import xlib.Window

interface WindowCoordinator {
    fun rearrangeActiveWindows()

    /**
     * @return measurements
     */
    fun addWindowToMonitor(windowId: Window): List<Int>

    fun removeWindow(windowId: Window)

    fun moveWindowToNextMonitor(windowId: Window)

    fun moveWindowToPreviousMonitor(windowId: Window)

    fun getMonitorForWindow(windowId: Window): Monitor

    fun getWindowMeasurements(windowId: Window): List<Int>
}