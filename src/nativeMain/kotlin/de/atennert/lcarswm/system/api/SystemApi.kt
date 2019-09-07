package de.atennert.lcarswm.system.api

/**
 * API that combines all other APIs for system access
 */
interface SystemApi: RandrApi, DrawApi, InputApi, EventApi, PosixApi