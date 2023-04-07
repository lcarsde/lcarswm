package de.atennert.lcarswm.atom

/**
 * Contains enum values for all used X atoms. Only exception is WM_S*
 */
enum class Atoms(val atomName: String) {
    WINDOW("WINDOW"),
    ATOM("ATOM"),
    STRING("STRING"),
    UTF_STRING("UTF8_STRING"),
    MANAGER("MANAGER"),
    COMPOUND_TEXT("COMPOUND_TEXT"),

    WM_DELETE_WINDOW("WM_DELETE_WINDOW"),
    WM_PROTOCOLS("WM_PROTOCOLS"),
    WM_STATE("WM_STATE"),
    WM_CHANGE_STATE("WM_CHANGE_STATE"),
    WM_CLASS("WM_CLASS"),
    WM_NAME("WM_NAME"),
    WM_TRANSIENT_FOR("WM_TRANSIENT_FOR"),

    NET_WM_NAME("_NET_WM_NAME"),
    NET_SUPPORTED("_NET_SUPPORTED"),
    NET_SUPPORTING_WM_CHECK("_NET_SUPPORTING_WM_CHECK"),
    NET_WM_DESKTOP("_NET_WM_DESKTOP"),
    NET_WM_STATE("_NET_WM_STATE"),
    NET_CLOSE_WINDOW("_NET_CLOSE_WINDOW"),
    NET_ACTIVE_WINDOW("_NET_ACTIVE_WINDOW"),
    NET_WM_MOVERESIZE("_NET_WM_MOVERESIZE"),
    NET_MOVERESIZE_WINDOW("_NET_MOVERESIZE_WINDOW"),
    NET_RESTACK_WINDOW("_NET_RESTACK_WINDOW"),
    NET_CLIENT_LIST("_NET_CLIENT_LIST"),
    NET_WM_WINDOW_TYPE("_NET_WM_WINDOW_TYPE"),
    NET_WM_WINDOW_TYPE_DESKTOP("_NET_WM_WINDOW_TYPE_DESKTOP"),
    NET_WM_WINDOW_TYPE_DOCK("_NET_WM_WINDOW_TYPE_DOCK"),
    NET_WM_WINDOW_TYPE_TOOLBAR("_NET_WM_WINDOW_TYPE_TOOLBAR"),
    NET_WM_WINDOW_TYPE_UTILITY("_NET_WM_WINDOW_TYPE_UTILITY"),
    NET_WM_WINDOW_TYPE_SPLASH("_NET_WM_WINDOW_TYPE_SPLASH"),
    NET_WM_WINDOW_TYPE_DIALOG("_NET_WM_WINDOW_TYPE_DIALOG"),
    NET_WM_WINDOW_TYPE_NORMAL("_NET_WM_WINDOW_TYPE_NORMAL"),
    NET_WM_WINDOW_TYPE_OVERRIDE("_KDE_NET_WM_WINDOW_TYPE_OVERRIDE"),

    LCARSDE_APP_MENU("LCARSDE_APP_MENU"),
    LCARSDE_STATUS_BAR("LCARSDE_STATUS_BAR")
}