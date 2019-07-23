package de.atennert.lcarswm

/**
 * Resource representing a physical monitor and its settings.
 */
data class Monitor(val id: UInt, val name: String) {

    var x: Short = 0
        private set

    var y: Short = 0
        private set

    var width: UShort = 0.toUShort()
        private set

    var height: UShort = 0.toUShort()
        private set

    private var valuesAreSet = false

    override fun hashCode(): Int {
        return id.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Monitor

        if (id != other.id) return false

        return true
    }

    /**
     * Update the monitor measurement settings.
     * @return true if any setting changed, false otherwise
     */
    fun setMeasurements(x: Short, y: Short, width: UShort, height: UShort) {
        if (this.valuesAreSet) {
            throw IllegalStateException("Tried to set values on monitor ${this.id}:$this.name but values are already set!")
        }

        this.x = x
        this.y = y
        this.width = width
        this.height = height

        this.valuesAreSet = true
    }

    /**
     * @return true when the other monitor has the same measurements as this monitor, false otherwise
     */
    fun hasDifferentMeasurements(other: Monitor): Boolean {
        return this.x != other.x || this.y != other.y || this.width != other.width || this.height != other.height
    }

    /**
     * Check if a monitor is a clone of this monitor.
     * @return true if it is a clone, false otherwise
     */
    fun isClone(other: Monitor): Boolean {
        return other.x == this.x && other.y == this.y
    }
}