package org.eclipse.lmos.cli.arc


import jakarta.ws.rs.core.Response
import org.eclipse.lmos.cli.agent.AgentType
import org.eclipse.lmos.cli.commands.agent.AgentInfo
import org.eclipse.lmos.cli.commands.agent.AgentStatus
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENT_PROJECTS_DIRECTORY
import org.eclipse.lmos.cli.llm.LLMConfig
import org.eclipse.lmos.cli.agent.AgentManager
import org.eclipse.lmos.cli.outbound.GenericRestClient
import org.eclipse.lmos.cli.utils.executeCommand
import org.eclipse.lmos.cli.utils.executeCommandStreaming
import org.eclipse.lmos.cli.utils.runAtFixedRate
import org.slf4j.LoggerFactory
import java.nio.file.Path


class ArcMacOSAgentManager : AgentManager {

    private val log = LoggerFactory.getLogger(ArcMacOSAgentManager::class.java)


    override fun startAgent(llmConfigs: List<LLMConfig>): AgentStatus {

        val agents = AGENT_PROJECTS_DIRECTORY.resolve(AgentType.ARC.name)
        if (getAgentAppStatus() == AgentStatus.READY) {
            return AgentStatus.READY
        }

        val envVars = getEnvVars(llmConfigs)
        val startCommand = createStartCommand(agents, envVars)
//        executeCommandStreaming(startCommand, 20, mutableListOf())
        executeCommand(startCommand, false)

        return getAgentStatus()
    }

    override fun getAgentStatus(): AgentStatus {
        val result2 = runAtFixedRate(
            pollingDurationMillis = 2000L,  // 1 second
            maxAttempts = 10,
            fn = { getAgentAppStatus() },
            fn2 = { tempResult -> isAgentReady(tempResult as AgentStatus) }
        )

        return if (result2 == AgentStatus.READY) {
            AgentStatus.READY
        } else {
            AgentStatus.FAILED
        }
    }

    private fun createStartCommand(
        agents: Path?,
        envVars: String
    ) = arrayOf(
        "sh", "-c", """
            cd $agents &&
            $envVars &&
            nohup ./gradlew -q --console=plain clean bootrun > application.log 2>&1 < /dev/null &
        """.trimIndent()
    )

    private fun getEnvVars(llmConfigs: List<LLMConfig>) =
        llmConfigs.mapIndexed { index, config ->
            """
            export ARC_AI_CLIENTS_${index}_ID=${config.id}
            export ARC_AI_CLIENTS_${index}_CLIENT=${config.provider}
            export ARC_AI_CLIENTS_${index}_URL=${config.baseUrl}
            export ARC_AI_CLIENTS_${index}_APIKEY=${config.apiKey}
            export ARC_AI_CLIENTS_${index}_MODELNAME=${config.modelName}
            """.trimIndent()
        }.joinToString("\n")

    val isAgentReady: (AgentStatus) -> Boolean = { it ->
        it == AgentStatus.READY
    }


    private fun getAgentAppStatus(): AgentStatus {
        val restClient = GenericRestClient()
        val response: Response
        try {
            response = restClient.create("http://localhost:9090/health").get()
        } catch (e: Exception) {
            log.error("Failed to connect to agent app", e)
            return AgentStatus.ERROR
        }
        if (response.statusInfo.family == Response.Status.Family.SUCCESSFUL) {
            return AgentStatus.READY
        }
        return AgentStatus.STARTING
    }

    override fun getLogs(agentInfo: AgentInfo): List<String> {
        return AGENT_PROJECTS_DIRECTORY.resolve(agentInfo.type.name).resolve("application.log").toFile().readLines()
            .toCollection(mutableListOf())
    }

    override fun shutdownAgent(agentInfo: AgentInfo) {
        val restClient = GenericRestClient()
        try {
            restClient.create("http://localhost:9090/shutdown").post("")
        } catch (e: Exception) {
            log.error("Failed to connect to agent app", e)
        }
    }
}