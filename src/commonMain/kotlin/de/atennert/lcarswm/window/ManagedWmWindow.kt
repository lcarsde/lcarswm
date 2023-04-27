package de.atennert.lcarswm.window

interface ManagedWmWindow<ID> : WmWindow<ID> {
    /** Frame window ID */
    val frame: ID
    /** Title bar window ID */
    val titleBar: ID
    /** Name of the program */
    val wmClass: String
    /** Window title (usually depends on program content) */
    val title: String
}