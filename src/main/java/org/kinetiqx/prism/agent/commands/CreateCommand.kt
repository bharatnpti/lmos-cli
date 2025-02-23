package org.kinetiqx.prism.agent.commands

import org.eclipse.lmos.starter.LmosAgentGeneratorService
import org.eclipse.lmos.starter.config.AgentConfig
import org.eclipse.lmos.starter.config.ProjectConfig
import org.eclipse.lmos.starter.factory.GradleSpringProjectFactory
import org.kinetiqx.prism.agent.AgentType
import org.kinetiqx.prism.constants.LmosCliConstants.AgentStarter.AGENT_PROJECTS_DIRECTORY
import org.kinetiqx.prism.constants.LmosCliConstants.AgentStarter.PACKAGE_NAME
import picocli.CommandLine

@CommandLine.Command(name = "create", description = ["Chat with the system"], mixinStandardHelpOptions = true)
class CreateCommand : Runnable {

    @CommandLine.Option(names = ["--an"], description = ["Agent name"])
    private var agentName: String? = null

    @CommandLine.Option(names = ["-t", "--type"], arity = "0..1", description = ["agent type"], completionCandidates = AgentType.Companion::class)
    private var type: AgentType? = null

    @CommandLine.Option(names = ["--am"], description = ["Agent model"])
    private var agentModel: String? = null

    @CommandLine.Option(names = ["--ad"], description = ["Agent descriptions"])
    private var agentDescriptions: String? = null

    @CommandLine.Option(names = ["--ap"], description = ["Agent prompt"])
    private var agentPrompt: String? = null

    override fun run() {

        type = type ?: AgentType.fromOrNull(promptUser("Enter agent type ${AgentType.entries}", true, AgentType.entries.map { it.name }.toTypedArray())) ?: AgentType.ARC
        agentName = agentName ?: promptUser("Enter agent name")
        agentModel = agentModel ?: promptUser("Enter agent model")
        agentDescriptions = agentDescriptions ?: promptUser("Enter agent descriptions")
        agentPrompt = agentPrompt ?: promptUser("Enter agent prompt")

        val projectConfig = ProjectConfig(AGENT_PROJECTS_DIRECTORY.toString(), PACKAGE_NAME, type!!.name)
        val agentConfig = AgentConfig(agentName!!, agentModel!!, agentDescriptions!!, agentPrompt!!)

        println("Generating agent project with the following details:")
        println("Project Config: $projectConfig")
        println("Agent Config: $agentConfig")

        LmosAgentGeneratorService().generateAgentProject(GradleSpringProjectFactory(), projectConfig, agentConfig)

        println("Agent Type: $type")
        // Implement the logic to handle the chat query here
    }

    private fun promptUser(message: String, optional: Boolean = false, values: Array<String>? = null): String? {
        var input: String?
        do {
            print("$message: ")
            input = readlnOrNull()?.takeIf { it.isNotBlank() }
            if (input == null && optional) return null
            if (input != null && (values == null || input in values)) return input
            println("Invalid value entered.")
        } while (true)
    }


}
