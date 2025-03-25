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
import org.eclipse.lmos.cli.utils.executeCommandWithProcessBuilder
import org.eclipse.lmos.cli.utils.runAtFixedRate
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ArcWindowsAgentManager : AgentManager {

    private val log = LoggerFactory.getLogger(ArcWindowsAgentManager::class.java)


    override fun startAgent(llmConfigs: List<LLMConfig>): AgentStatus {

        val agents = AGENT_PROJECTS_DIRECTORY.resolve(AgentType.ARC.name)
        if (getAgentAppStatus() == AgentStatus.READY) {
            return AgentStatus.READY
        }

        val envVars = getEnvVarsMap(llmConfigs)
//        val startCommand = createStartCommandWindows(agents, envVars)
        val command = listOf("cmd", "/c", "gradlew.bat", "-q", "--console=plain", "bootrun")

        println("Start command: ${command.joinToString(" ")}")
        println("agents: ${agents.toFile()}")
//        executeCommand(startCommand, false)
        executeCommandWithProcessBuilder(command, envVars, agents.toFile(), false)


        return getAgentStatus()
    }

    override fun getAgentStatus(): AgentStatus {
        println("entered getAgentStatus")
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

    private fun createStartCommandWindows(
        agents: Path?,
        envVars: String
    ) = arrayOf(
        "cmd", "/c", """
        cd /d ${agents?.toAbsolutePath()} && 
        $envVars && 
        start "" cmd /c "gradlew.bat -q --console=plain bootrun > application.log 2>&1"
    """.trimIndent()
    )


    private fun getEnvVarsMap(llmConfigs: List<LLMConfig>): Map<String, String> {
        return llmConfigs.flatMapIndexed { index, config ->
            listOf(
                "ARC_AI_CLIENTS_${index}_ID" to config.id,
                "ARC_AI_CLIENTS_${index}_CLIENT" to config.provider,
                "ARC_AI_CLIENTS_${index}_URL" to config.baseUrl,
                "ARC_AI_CLIENTS_${index}_APIKEY" to config.apiKey,
                "ARC_AI_CLIENTS_${index}_MODELNAME" to config.modelName
            )
        }.toMap()
    }

    private fun getEnvVars(llmConfigs: List<LLMConfig>) =
        llmConfigs.mapIndexed { index, config ->
            """
            set ARC_AI_CLIENTS_${index}_ID=${config.id}
            set ARC_AI_CLIENTS_${index}_CLIENT=${config.provider}
            set ARC_AI_CLIENTS_${index}_URL=${config.baseUrl}
            set ARC_AI_CLIENTS_${index}_APIKEY=${config.apiKey}
            set ARC_AI_CLIENTS_${index}_MODELNAME=${config.modelName}
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
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            val zonedDateTime = ZonedDateTime.now(TimeZone.getTimeZone("Asia/Kolkata").toZoneId())

            val formattedTime = zonedDateTime.format(formatter)
            log.error("$formattedTime: Failed to connect to agent app", e)
            println("$formattedTime: Failed to connect to agent app: $e")
            return AgentStatus.ERROR
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        val zonedDateTime = ZonedDateTime.now(TimeZone.getTimeZone("Asia/Kolkata").toZoneId())

        val formattedTime = zonedDateTime.format(formatter)
        println("$formattedTime: getAgentAppStatus Response: ${response.status}")
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