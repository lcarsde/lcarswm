package de.atennert.lcarswm.system

import de.atennert.lcarswm.system.api.SystemApi

class SystemAccess private constructor() {

    companion object {
        private var instance: SystemApi? = null

        /**
         * Access the system facade.
         */
        fun getInstance(): SystemApi {
            if (this.instance == null) {
                this.instance = SystemFacade()
            }

            return this.instance!!
        }

        /**
         * This method is for testing purposes only
         */
        fun overrideInstanceForTesting(instance: SystemApi) {
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