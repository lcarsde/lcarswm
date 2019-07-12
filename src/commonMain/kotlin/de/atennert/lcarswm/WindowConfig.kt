package de.atennert.lcarswm

/**
 * POJO containing window data
 */
class WindowConfig(
    private val x: Long,
    private val y: Long,
    private val width: Long,
    private val height: Long,
    private val stackMode: Long,
    private val sibling: Long /* mapping xcb_window_t */
) {
    fun getValue(config: XcbConfigWindow): Long {
        return when (config) {
            XcbConfigWindow.X -> this.x
            XcbConfigWindow.Y -> this.y
            XcbConfigWindow.WIDTH -> this.width
            XcbConfigWindow.HEIGHT -> this.height
            XcbConfigWindow.STACK_MODE -> this.stackMode
            XcbConfigWindow.SIBLING -> this.sibling
        }
    }
}

/**
 * TODO description
 */
fun configureWindow(mask: Int, wc: WindowConfig): Pair<Int, ArrayList<Long>> {
    return XcbConfigWindow.values()
        .filter { it.isInValue(mask) }
        .fold(Pair(0, ArrayList(7)))
        { (mask, values), config ->
            val newMask = mask or config.mask
            values.add(wc.getValue(config))
            Pair(newMask, values)
        }
}
