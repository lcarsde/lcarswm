package de.atennert.lcarswm

/**
 *
 */
interface Properties {
    operator fun get(propertyKey: String): String?
}