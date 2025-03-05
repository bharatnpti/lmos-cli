package org.eclipse.lmos.cli

import jakarta.inject.Singleton
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENTS_REGISTRY
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENT_PROJECTS_DIRECTORY
import org.eclipse.lmos.cli.constants.LmosCliConstants.CredentialManagerConstants.CREDENTIAL_DIRECTORY
import org.eclipse.lmos.cli.constants.LmosCliConstants.PROJECT_ROOT_DIR
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.createDirectories

@Singleton
class Initializer {

    private val log = LoggerFactory.getLogger(Initializer::class.java)

    fun initialize(): Int {
        println("Initializing LMOS CLI")
        ensureDirectories()
        return 0
    }

    private fun ensureDirectories() {
        if (Files.notExists(AGENT_PROJECTS_DIRECTORY)) {
            AGENT_PROJECTS_DIRECTORY.createDirectories()
        }
        if (Files.notExists(AGENTS_REGISTRY)) {
            AGENTS_REGISTRY.createDirectories()
        }
        if (!CREDENTIAL_DIRECTORY.toFile().exists()) {
            CREDENTIAL_DIRECTORY.createDirectories()
        }
    }
}