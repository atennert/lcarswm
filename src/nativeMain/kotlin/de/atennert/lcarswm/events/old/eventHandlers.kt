package de.atennert.lcarswm.events.old

import de.atennert.lcarswm.*
import de.atennert.lcarswm.log.Logger
import de.atennert.lcarswm.system.api.SystemApi
import kotlinx.cinterop.*
import xlib.*

/**
 * Map of event types to event handlers. DON'T EDIT THE MAPS CONTENT!!!
 */
val EVENT_HANDLERS =
    hashMapOf<Int, Function7<SystemApi, Logger, WindowManagerState, XEvent, CPointer<XImage>, Window, List<GC>, Boolean>>(
        Pair(KeyPress, ::handleKeyPress),
        Pair(KeyRelease, ::handleKeyRelease),
        Pair(ButtonPress, { _, l, _, e, _, _, _ -> logButtonPress(l, e) }),
        Pair(ButtonRelease, { _, l, _, e, _, _, _ -> logButtonRelease(l, e) }),
        Pair(ConfigureRequest, { s, l, w, e, _, _, _ ->
            handleConfigureRequest(
                s,
                l,
                w,
                e
            )
        }),
        Pair(MapRequest, { s, l, w, e, _, rw, _ -> handleMapRequest(s, l, w, e, rw) }),
        Pair(MapNotify, { _, l, _, e, _, _, _ -> logMapNotify(l, e) }),
        Pair(DestroyNotify, { s, l, w, e, _, _, _ -> handleDestroyNotify(s, l, w, e) }),
        Pair(UnmapNotify, ::handleUnmapNotify),
        Pair(ReparentNotify, { _, l, _, e, _, _, _ -> logReparentNotify(l, e) }),
        Pair(CreateNotify, { _, l, _, e, _, _, _ -> logCreateNotify(l, e) }),
        Pair(ConfigureNotify, { _, l, _, e, _, _, _ -> logConfigureNotify(l, e) }),
        Pair(ClientMessage, {_, l, _, e, _, _, _ -> logClientMessage(l, e) })
    )