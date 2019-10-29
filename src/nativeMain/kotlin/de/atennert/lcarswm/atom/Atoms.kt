package de.atennert.lcarswm.atom

/**
 * Contains enum values for all used X atoms. Only exception is WM_S*
 */
enum class Atoms(val atomName: String) {
    WINDOW("WINDOW"),
    ATOM("ATOM"),
    UTF_STRING("UTF8_STRING"),

    WM_DELETE_WINDOW("WM_DELETE_WINDOW"),
    WM_PROTOCOLS("WM_PROTOCOLS"),
    WM_STATE("WM_STATE"),
    WM_CLASS("WM_CLASS"),

    NET_WM_NAME("_NET_WM_NAME"),
    NET_SUPPORTED("_NET_SUPPORTED"),
    NET_SUPPORTING_WM_CHECK("_NET_SUPPORTING_WM_CHECK")
}