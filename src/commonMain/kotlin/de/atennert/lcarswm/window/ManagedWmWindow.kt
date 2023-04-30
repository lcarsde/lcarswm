package de.atennert.lcarswm.window

interface ManagedWmWindow<ID> : WmWindow<ID> {
    /** Frame window ID */
    val frame: ID
    /** Name of the program */
    val wmClass: String
    /** Window title (usually depends on program content) */
    val title: String

    fun hasId(windowId: ID): Boolean = id == windowId || frame == windowId

    fun isTitleBar(windowId: ID): Boolean = false
}