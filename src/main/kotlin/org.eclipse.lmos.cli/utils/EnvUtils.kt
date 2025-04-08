package org.eclipse.lmos.cli.utils

import org.eclipse.lmos.cli.utils.CliPrinter.printlnHeader

class EnvUtils {
    companion object {
        // Read environment variable at build time and bake it into the binary
        private val LMOS_CLI_SECRET_KEY: String = getSecretKey()

        @JvmStatic
        fun getLmosCliSecretKey(): String {
            printlnHeader("LMOS_CLI_SECRET_KEY $LMOS_CLI_SECRET_KEY")
            return LMOS_CLI_SECRET_KEY
        }

        private fun getSecretKey(): String {
            val key = System.getenv("LMOS_CLI_SECRET_KEY")
                ?: throw IllegalStateException("LMOS_CLI_SECRET_KEY must be set at build time!")
            return key
        }
    }
}
