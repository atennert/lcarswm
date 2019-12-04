package de.atennert.lcarswm.windowactions

import de.atennert.lcarswm.ScreenMode
import de.atennert.lcarswm.monitor.Monitor
import de.atennert.lcarswm.system.FunctionCall
import kotlinx.cinterop.convert
import xlib.Window

class WindowCoordinatorMock : WindowCoordinator {
    val functionCalls = mutableListOf<FunctionCall>()

    override fun rearrangeActiveWindows() {
        functionCalls.add(FunctionCall("rearrangeActiveWindows"))
    }

    val primaryMonitor = Monitor(21.convert(), "", true)
    override fun addWindowToMonitor(windowId: Window): List<Int> {
        functionCalls.add(FunctionCall("addWindowToMonitor", windowId))
        return primaryMonitor.getCurrentWindowMeasurements(ScreenMode.NORMAL)
    }

    override fun removeWindow(windowId: Window) {
        functionCalls.add(FunctionCall("removeWindow", windowId))
    }

    override fun moveWindowToNextMonitor(windowId: Window) {
        functionCalls.add(FunctionCall("moveWindowToNextMonitor", windowId))
    }

    override fun moveWindowToPreviousMonitor(windowId: Window) {
        functionCalls.add(FunctionCall("moveWindowToPreviousMonitor", windowId))
    }

    override fun getMonitorForWindow(windowId: Window): Monitor {
        return Monitor(1.convert(), "some monitor", false)
    }

    override fun getWindowMeasurements(windowId: Window): List<Int> {
        return primaryMonitor.getCurrentWindowMeasurements(ScreenMode.NORMAL)
    }
}