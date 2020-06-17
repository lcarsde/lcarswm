package de.atennert.lcarswm

import de.atennert.lcarswm.system.api.PosixApi
import kotlinx.cinterop.CPointer
import platform.posix.FILE

object SettingsWriter {
    /*
     * I would prefer to put this into resources so it could be accessed by something
     * like getResource(...), but that's currently not available on Kotlin native.
     */
    private const val INITIAL_SETTINGS = """<?xml version="1.0" encoding="UTF-8"?>
<lcarswm>
    <key-config>
        <!-- keys for executing programs -->
        <binding>
            <keys>Lin+T</keys>
            <exec>lxterminal</exec>
        </binding>
        <binding>
            <keys>Lin+B</keys>
            <exec>firefox</exec>
        </binding>
        <binding>
            <keys>XF86AudioMute</keys>
            <exec>amixer set Master toggle</exec>
        </binding>
        <binding>
            <keys>XF86AudioRaiseVolume</keys>
            <exec>amixer set Master 3%+</exec>
        </binding>
        <binding>
            <keys>XF86AudioLowerVolume</keys>
            <exec>amixer set Master 3%-</exec>
        </binding>

        <!-- Window manager action keys ... only edit the following when you know what you're doing -->
        <binding>
            <keys>Alt+Tab</keys>
            <action>window-toggle-forward</action>
        </binding>
        <binding>
            <keys>Alt+Up</keys>
            <action>window-move-up</action>
        </binding>
        <binding>
            <keys>Alt+Down</keys>
            <action>window-move-down</action>
        </binding>
        <binding>
            <keys>Alt+F4</keys>
            <action>window-close</action>
        </binding>
        <binding>
            <keys>Lin+M</keys>
            <action>screen-mode-toggle</action>
        </binding>
        <binding>
            <keys>Lin+Q</keys>
            <action>lcarswm-quit</action>
        </binding>
    </key-config>
    <general>
        <title>LCARS</title><!-- Will be shown when there's no title image -->
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