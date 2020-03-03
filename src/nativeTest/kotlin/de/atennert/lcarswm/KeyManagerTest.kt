package de.atennert.lcarswm

import de.atennert.lcarswm.system.SystemFacadeMock
import kotlinx.cinterop.convert
import xlib.ControlMask
import xlib.LockMask
import xlib.ShiftMask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyManagerTest {
    @Test
    fun `ungrab all keys`() {
        val systemApi = SystemFacadeMock()

        KeyManager(systemApi, systemApi.rootWindowId).ungrabAllKeys()

        val ungrabKeysCall = systemApi.functionCalls.removeAt(0)

        assertEquals("ungrabKey", ungrabKeysCall.name, "The keys need to be initially ungrabbed")
        assertEquals(systemApi.rootWindowId, ungrabKeysCall.parameters[0], "The keys need to be ungrabbed for the root window")
    }

    @Test
    fun `load modifier keys`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        val expectedModifiers = mutableMapOf(
            Pair(Modifiers.CAPS_LOCK, LockMask),
            Pair(Modifiers.SHIFT, ShiftMask),
            Pair(Modifiers.CONTROL, ControlMask),
            Pair(Modifiers.ALT, 0x8),
            Pair(Modifiers.HYPER, 0x10),
            Pair(Modifiers.META, 0x20),
            Pair(Modifiers.SUPER, 0x40),
            Pair(Modifiers.SCROLL_LOCK, 0x80)
        )

        assertEquals(expectedModifiers, keyManager.modMasks, "The KeyManager should get the required modifier keys")
    }

    @Test
    fun `grab input controls`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)
        systemApi.functionCalls.clear()

        keyManager.grabInternalKeys()

        val inputCalls = systemApi.functionCalls

        LCARS_WM_KEY_SYMS
            .filterNot { systemApi.keySyms[it] == 0 } // 0s are not available
            .onEach { keySym ->
                val grabKeyCall = inputCalls.removeAt(0)
                assertEquals("grabKey", grabKeyCall.name, "The modifier key needs to be grabbed")
                assertEquals(
                    systemApi.keySyms[keySym],
                    grabKeyCall.parameters[0],
                    "The key needs to be ${systemApi.keySyms[keySym]}"
                )
                assertEquals(
                    0x40.toUInt(),
                    grabKeyCall.parameters[1],
                    "The modifier key needs to be SUPER (Win)"
                )
                assertEquals(
                    systemApi.rootWindowId,
                    grabKeyCall.parameters[2],
                    "The key needs to be grabbed for the root window"
                )
            }
            .forEach { keySym ->
                assertEquals(
                    keySym,
                    keyManager.getKeySym(systemApi.keySyms.getValue(keySym).convert())!!.convert()
                )
            }
    }

    @Test
    fun `return null on unknown key`() {
        val systemApi = SystemFacadeMock()

        val keyManager = KeyManager(systemApi, systemApi.rootWindowId)

        assertNull(keyManager.getKeySym((-1).convert()), "The key manager should return null for unknown key codes")
    }
}