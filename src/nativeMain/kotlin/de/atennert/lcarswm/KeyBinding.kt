package de.atennert.lcarswm

sealed class KeyBinding {
    abstract val keys: String
    abstract val command: String
}

/** The key execution defines a key binding for executing a command line command with exec */
data class KeyExecution(override val keys: String, override val command: String) : KeyBinding()

/** The key action defines a key binding for a window manager action */
data class KeyAction(override val keys: String, override val command: String) : KeyBinding()