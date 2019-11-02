package de.atennert.lcarswm.events

import de.atennert.lcarswm.WindowManagerStateHandler
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import xlib.DestroyNotify
import xlib.XEvent

/**
 *
 */
class DestroyNotifyHandler(
    private val system: SystemApi,
    private val logger: Logger,
    private val windowManagerState: WindowManagerStateHandler
) : XEventHandler {
    override val xEventType = DestroyNotify

    override fun handleEvent(event: XEvent): Boolean {
        val destroyedWindow = event.xdestroywindow.window
        logger.logDebug("DestroyNotifyHandler::handleEvent::clean up after destroyed window: $destroyedWindow")
        if (windowManagerState.hasWindow(destroyedWindow)) {
            val window = windowManagerState.windows.map { it.first }.single { it.id == destroyedWindow }
            system.unmapWindow(window.frame)
            system.removeFromSaveSet(destroyedWindow)
            system.destroyWindow(window.frame)

            windowManagerState.removeWindow(destroyedWindow)
        }
        return false
    }
}