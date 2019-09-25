package de.atennert.lcarswm

import xlib.Window

interface WindowManagerStateHandler {
    val initialMonitor: Monitor

    fun addWindow(window: WindowContainer, monitor: Monitor)

    fun removeWindow(windowId: Window)

    fun getScreenModeForMonitor(monitor: Monitor): ScreenMode
}