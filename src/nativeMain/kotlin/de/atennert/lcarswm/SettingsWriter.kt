package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.CPointer
import platform.posix.FILE

class SettingsWriter {
    companion object {
        /*
         * I would prefer to put this into resources so it could be accessed by something
         * like get resource, but that's not available on Kotlin native.
         */
        private const val INITIAL_SETTINGS = """<?xml version="1.0" encoding="UTF-8"?>
<lcarswm>
    <key-config>
        <!-- Bindings for executing programs -->
        <entry>
            <binding>Lin+T</binding>
            <command>lxterminal</command>
        </entry>
        <entry>
            <binding>Lin+B</binding>
            <command>firefox</command>
        </entry>
        <entry>
            <binding>XF86AudioMute</binding>
            <command>amixer set Master toggle</command>
        </entry>
        <entry>
            <binding>XF86AudioRaiseVolume</binding>
            <command>amixer set Master 3%+</command>
        </entry>
        <entry>
            <binding>XF86AudioLowerVolume</binding>
            <command>amixer set Master 3%-</command>
        </entry>

        <!-- Window manager action bindings -->
        <entry>
            <binding>Alt+Tab</binding>
            <action>window-toggle-forward</action>
        </entry>
        <entry>
            <binding>Alt+Up</binding>
            <action>window-move-up</action>
        </entry>
        <entry>
            <binding>Alt+Down</binding>
            <action>window-move-down</action>
        </entry>
        <entry>
            <binding>Alt+F4</binding>
            <action>window-close</action>
        </entry>
        <entry>
            <binding>Lin+M</binding>
            <action>screen-mode-toggle</action>
        </entry>
        <entry>
            <binding>Lin+Q</binding>
            <action>lcarswm-quit</action>
        </entry>
    </key-config>
    <general>
        <title>LCARS</title>
        <!--<title-image>/usr/share/pixmaps/lcarswm.xpm</title-image>-->
        <font>Ubuntu Condensed</font>
    </general>
</lcarswm>
        """

        fun writeInitialSettings(posixApi: PosixApi, settingsFilePath: String): Boolean {
            val file: CPointer<FILE> = posixApi.fopen(settingsFilePath, "w") ?: return false

            posixApi.fputs(INITIAL_SETTINGS, file)

            posixApi.fclose(file)
            return true
        }
    }
}