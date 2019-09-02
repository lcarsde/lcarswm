package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemDrawApi

/**
 * Proxy for accessing system functions.
 */
class SystemProxy private constructor() {

    companion object {
        private var instance: SystemDrawApi? = null

        /**
         * Access the proxy implementation.
         */
        fun getInstance(): SystemDrawApi {
            if (this.instance == null) {
                this.instance = SystemFacade()
            }

            return this.instance!!
        }

        /**
         * This method is for testing purposes only
         */
        fun testOverrideInstance(instance: SystemDrawApi) {
            this.instance = instance
        }

        /**
         * Clean up the proxy instance.
         */
        fun clear() {
            this.instance = null
        }
    }
}