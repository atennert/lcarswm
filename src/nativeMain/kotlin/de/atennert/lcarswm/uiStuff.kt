package de.atennert.lcarswm

import cnames.structs.xcb_connection_t
import kotlinx.cinterop.*
import xcb.*

val COLORS = listOf(
    Triple(0, 0, 0),
    Triple(0x9999, 0x9999, 0xffff)
)

val DRAW_FUNCTIONS =
    hashMapOf(
        Pair(ScreenMode.NORMAL, ::drawNormalFrame),
        Pair(ScreenMode.MAXIMIZED, ::drawMaximizedFrame),
        Pair(ScreenMode.FULLSCREEN, ::clearScreen)
    )

fun allocateColorMap(
    xcbConnection: CPointer<xcb_connection_t>,
    rootVisual: UInt,
    windowId: UInt
): Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>> {

    val colorMapId = xcb_generate_id(xcbConnection)

    xcb_create_colormap(xcbConnection, XCB_COLORMAP_ALLOC_NONE.convert(), colorMapId, windowId, rootVisual)

    val colorReplies = COLORS
        .asSequence()
        .map { (red, green, blue) ->
            xcb_alloc_color(xcbConnection, colorMapId, red.convert(), green.convert(), blue.convert())
        }
        .map { colorCookie -> xcb_alloc_color_reply(xcbConnection, colorCookie, null) }
        .onEach { println("color: $it") }
        .filterNotNull()
        .toList()

    return Pair(colorMapId, colorReplies)
}

fun getGraphicContexts(
    xcbConnection: CPointer<xcb_connection_t>,
    rootId: UInt,
    colors: List<CPointer<xcb_alloc_color_reply_t>>
): List<UInt> = colors
    .map { colorReply ->
        val gcId = xcb_generate_id(xcbConnection)
        val parameterArray = arrayOf(colorReply.pointed.pixel, 0.convert(), XCB_ARC_MODE_PIE_SLICE.convert())
        val parameters = UIntArray(parameterArray.size) { parameterArray[it] }
        xcb_create_gc(
            xcbConnection,
            gcId,
            rootId,
            XCB_GC_FOREGROUND or XCB_GC_GRAPHICS_EXPOSURES or XCB_GC_ARC_MODE,
            parameters.toCValues()
        )
        gcId
    }

fun cleanupColorMap(
    xcbConnection: CPointer<xcb_connection_t>,
    colorMap: Pair<UInt, List<CPointer<xcb_alloc_color_reply_t>>>
) {
    xcb_free_colormap(xcbConnection, colorMap.first)

    colorMap.second.forEach { colorReply -> nativeHeap.free(colorReply) }
}

private fun drawMaximizedFrame(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState
) {
    clearScreen(xcbConnection, windowManagerState)

    val graphicsContext = windowManagerState.graphicsContexts[1]

    windowManagerState.monitors.forEach {monitor ->
        val arcs = nativeHeap.allocArray<xcb_arc_t>(4)
        for (i in 0 until 4) {
            arcs[i].width = 40.toUShort()
            arcs[i].height = 40.toUShort()
        }
        arcs[0].x = monitor.x.toShort()
        arcs[0].y = monitor.y.toShort()
        arcs[0].angle1 = 90.shl(6)
        arcs[0].angle2 = 180.shl(6)

        arcs[1].x = monitor.x.toShort()
        arcs[1].y = (monitor.y + monitor.height - 40).toShort()
        arcs[1].angle1 = 90.shl(6)
        arcs[1].angle2 = 180.shl(6)

        arcs[2].x = (monitor.x + monitor.width - 40).toShort()
        arcs[2].y = monitor.y.toShort()
        arcs[2].angle1 = 270.shl(6)
        arcs[2].angle2 = 180.shl(6)

        arcs[3].x = (monitor.x + monitor.width - 40).toShort()
        arcs[3].y = (monitor.y + monitor.height - 40).toShort()
        arcs[3].angle1 = 270.shl(6)
        arcs[3].angle2 = 180.shl(6)

        val rects = nativeHeap.allocArray<xcb_rectangle_t>(6)
        // extensions for round pieces
        for (i in 0 until 4) {
            rects[i].width = 12.toUShort()
            rects[i].height = 40.toUShort()
        }
        rects[0].x = (monitor.x + 20).toShort()
        rects[0].y = monitor.y.toShort()

        rects[1].x = (monitor.x + 20).toShort()
        rects[1].y = (monitor.y + monitor.height - 40).toShort()

        rects[2].x = (monitor.x + monitor.width - 32).toShort()
        rects[2].y = monitor.y.toShort()

        rects[3].x = (monitor.x + monitor.width - 32).toShort()
        rects[3].y = (monitor.y + monitor.height - 40).toShort()

        // top bar
        rects[4].x = (monitor.x + 120).toShort()
        rects[4].y = monitor.y.toShort()
        rects[4].width = (monitor.width - 160).toUShort()
        rects[4].height = 40.toUShort()

        // bottom bar
        rects[5].x = (monitor.x + 40).toShort()
        rects[5].y = (monitor.y + monitor.height - 40).toShort()
        rects[5].width = (monitor.width - 80).toUShort()
        rects[5].height = 40.toUShort()

        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 4.convert(), arcs)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 6.convert(), rects)

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
    }
}

private fun drawNormalFrame(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState
) {
    clearScreen(xcbConnection, windowManagerState)

    val graphicsContext = windowManagerState.graphicsContexts[1]

    windowManagerState.monitors.forEach {monitor ->
        val arcs = nativeHeap.allocArray<xcb_arc_t>(3)
        for (i in 0 until 3) {
            arcs[i].width = 40.toUShort()
            arcs[i].height = 40.toUShort()
        }
        arcs[0].x = (monitor.x + monitor.width - 40).toShort()
        arcs[0].y = monitor.y.toShort()
        arcs[0].angle1 = 270.shl(6)
        arcs[0].angle2 = 180.shl(6)

        arcs[1].x = (monitor.x + monitor.width - 40).toShort()
        arcs[1].y = (monitor.y + 176).toShort()
        arcs[1].angle1 = 270.shl(6)
        arcs[1].angle2 = 180.shl(6)

        arcs[2].x = (monitor.x + monitor.width - 40).toShort()
        arcs[2].y = (monitor.y + monitor.height - 40).toShort()
        arcs[2].angle1 = 270.shl(6)
        arcs[2].angle2 = 180.shl(6)

        val rects = nativeHeap.allocArray<xcb_rectangle_t>(3)
        // extensions for round pieces
        for (i in 0 until 3) {
            rects[i].width = 12.toUShort()
            rects[i].height = 40.toUShort()
        }
        rects[0].x = (monitor.x + monitor.width - 32).toShort()
        rects[0].y = monitor.y.toShort()

        rects[1].x = (monitor.x + monitor.width - 32).toShort()
        rects[1].y = (monitor.y + 176).toShort()

        rects[2].x = (monitor.x + monitor.width - 32).toShort()
        rects[2].y = (monitor.y + monitor.height - 40).toShort()

        xcb_poly_fill_arc(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 3.convert(), arcs)
        xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 3.convert(), rects)

        nativeHeap.free(arcs)
        nativeHeap.free(rects)
    }
}

private fun clearScreen(
    xcbConnection: CPointer<xcb_connection_t>,
    windowManagerState: WindowManagerState
) {
    val rect = nativeHeap.alloc<xcb_rectangle_t>()

    rect.x = 0
    rect.y = 0
    rect.width = windowManagerState.screenSize.first.convert()
    rect.height = windowManagerState.screenSize.second.convert()

    val graphicsContext = windowManagerState.graphicsContexts[0]

    xcb_poly_fill_rectangle(xcbConnection, windowManagerState.lcarsWindowId, graphicsContext, 1.convert(), rect.ptr)

    nativeHeap.free(rect)
}