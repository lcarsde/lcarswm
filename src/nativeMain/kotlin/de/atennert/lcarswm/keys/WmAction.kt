package de.atennert.lcarswm.keys

/**
 * Window manager internal actions.
 */
enum class WmAction(val key: String) {
    WINDOW_TOGGLE_FWD("window-toggle-forward"),
    WINDOW_TOGGLE_BWD("window-toggle-backward"),
    WINDOW_MOVE_UP("window-move-up"),
    WINDOW_MOVE_DOWN("window-move-down"),
    WINDOW_CLOSE("window-close"),
    SCREEN_MODE_TOGGLE("screen-mode-toggle"),
    WM_QUIT("lcarswm-quit");

    companion object {
        fun getActionByKey(key: String): WmAction? {
            return values().find { it.key == key }
        }
    }
}