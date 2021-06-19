package de.atennert.lcarswm.keys

/**
 * Window manager internal actions.
 */
enum class WmAction(val keys: Set<String>) {
    WINDOW_TOGGLE_FWD(setOf("window-toggle-forward")),
    WINDOW_TOGGLE_BWD(setOf("window-toggle-backward")),
    WINDOW_MOVE_NEXT(setOf("window-move-next", "window-move-up")),
    WINDOW_MOVE_PREVIOUS(setOf("window-move-previous", "window-move-down")),
    WINDOW_CLOSE(setOf("window-close")),
    SCREEN_MODE_TOGGLE(setOf("screen-mode-toggle")),
    WM_QUIT(setOf("lcarswm-quit"));

    companion object {
        fun getActionByKey(key: String): WmAction? {
            return values().find { it.keys.contains(key) }
        }
    }
}