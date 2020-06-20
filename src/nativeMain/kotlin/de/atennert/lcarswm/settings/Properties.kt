package de.atennert.lcarswm.settings

/**
 * Interfaces for classes that load and provide Java-style properties.
 */
interface Properties {
    /**
     * @return the property value for a given key, returns null, if the key is not a known property
     */
    operator fun get(propertyKey: String): String?

    /**
     * @return the names of all loaded properties
     */
    fun getPropertyNames(): Set<String>
}