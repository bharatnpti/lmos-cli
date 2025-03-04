package org.eclipse.lmos.cli

import jakarta.inject.Singleton
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENTS_REGISTRY
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENT_PROJECTS_DIRECTORY
import org.eclipse.lmos.cli.constants.LmosCliConstants.CredentialManagerConstants.CREDENTIAL_CONFIG
import org.eclipse.lmos.cli.constants.LmosCliConstants.CredentialManagerConstants.CREDENTIAL_DIRECTORY
import org.eclipse.lmos.cli.constants.LmosCliConstants.CredentialManagerConstants.MODEL_IDS
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

@Singleton
class Initializer {

    fun initialize() {
        println("Initializing Lmos CLI")
        ensureDirectories()
        ensureConfigs()
    }

    private fun ensureConfigs() {
        if (!CREDENTIAL_CONFIG.exists()) {
            val createNewFile = CREDENTIAL_CONFIG.createNewFile()
            println("Cred file created: $createNewFile")
            CREDENTIAL_CONFIG.setWritable(true, true)
        }

        if (!MODEL_IDS.exists()) {
            val createNewFile = MODEL_IDS.createNewFile()
            println("MODEL_IDS file created: $createNewFile")
            MODEL_IDS.setWritable(true, true)
        }
    }

    private fun ensureDirectories() {
        if (AGENT_PROJECTS_DIRECTORY.notExists()) {
            AGENT_PROJECTS_DIRECTORY.createDirectories()
            println("Cred directory created: $AGENT_PROJECTS_DIRECTORY")
        }
        if (AGENTS_REGISTRY.notExists()) {
            AGENTS_REGISTRY.createDirectories()
            println("Cred directory created: $AGENTS_REGISTRY")
        }
        if (!CREDENTIAL_DIRECTORY.toFile().exists()) {
            val createDirectories = CREDENTIAL_DIRECTORY.createDirectories()
            println("Cred directory created: $createDirectories")
        }
    }
}