package de.atennert.lcarswm

/**
 * Implementation of xcb_config_window_t
 */
enum class XcbConfigWindow(val mask: Int) {
    X(1),
    Y(2),
    WIDTH(4),
    HEIGHT(8),
//    BORDER_WIDTH(16), // not interesting for us
    SIBLING(32),
    STACK_MODE(64);

    fun isInValue(value: Int): Boolean = this.mask == (value and this.mask)
}