package org.eclipse.lmos.cli.commands.agent

import kotlinx.serialization.Serializable
import org.eclipse.lmos.cli.agent.AgentType
import org.eclipse.lmos.cli.constants.LmosCliConstants.AgentStarterConstants.AGENT_PROJECTS_DIRECTORY
import org.eclipse.lmos.cli.registry.agent.AgentRegistry
import picocli.CommandLine
import java.io.*
import kotlin.collections.List

@CommandLine.Command(name = "chat", description = ["Chat with the system"], mixinStandardHelpOptions = true)
class Chat : Runnable {

    @CommandLine.Option(names = ["--an"], description = ["Agent name"])
    private var agentName: String? = null

    private val history = mutableListOf<Pair<String, String>>()  // Stores input-response pairs

    override fun run() {

        println("Chatting with query: $agentName")

        agentName?.let { while (true) {

            val startStatus = run { startProject() }

            if(startStatus == "STARTED") {

                val agentRegistry = AgentRegistry()
                val agentInfo: AgentInfo = agentRegistry.findAgent(it)

                val input = promptUser("Enter your query")!!

                if (input.equals("exit", ignoreCase = true)) {
                    println("Exiting session...")
                    break
                }

                val response = processInput(input, agentInfo)  //todo agent-integrator
                history.add(input to response)
                println("Response: $response")
            } else {
                println("Error in starting Agents, consult logs")
            }
        } } ?: "Chat with LLM" //todo integrate llm call


    }

    private fun startProject(): String {
        val projectDirectory = AGENT_PROJECTS_DIRECTORY.resolve(AgentType.ARC.name)

        val args = arrayOf("")  //todo fetch config from credential manager

        val command = "cd $projectDirectory && nohup ./gradlew -q --console=plain bootrun --args='$args'>application.log 2>&1 < /dev/null &"   //macos

        val windowsCommand = "cd $projectDirectory && start /b /min \"\" gradlew -q --console=plain bootrun --args=\"$args\" > application.log 2>&1"

        //todo execute command and return result

        return "Started"
    }

    private fun processInput(input: String, agentInfo: AgentInfo): String {

        val agentCommunicator: AgentCommunicator? = null
        agentCommunicator?.connect()
        if(AgentStatus.READY == agentCommunicator?.checkStatus()) {
            agentCommunicator.sendCommand(input)
        }
        agentCommunicator?.disconnect()

        return "Answer: $input"
    }
}

interface AgentDiscovery {

    fun discoverAgents(): List<AgentInfo>

}

interface AgentCommunicator {
    fun connect()
    fun checkStatus(): AgentStatus
    fun sendCommand(command: String): String
    fun disconnect()
}

enum class AgentStatus {
    STARTING, READY, FAILED
}


class ConsoleIOAdapter(private val processBuilder: ProcessBuilder) : AgentCommunicator {
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    override fun connect() {
        process = processBuilder.start()
        reader = BufferedReader(InputStreamReader(process?.inputStream))
        writer = BufferedWriter(OutputStreamWriter(process?.outputStream))
    }

    override fun checkStatus(): AgentStatus {
        TODO("Not yet implemented")
    }

    override fun sendCommand(command: String): String {
        writer?.write(command)
        writer?.newLine()
        writer?.flush()
        return reader?.readLine() ?: ""
    }

    override fun disconnect() {
        reader?.close()
        writer?.close()
        process?.destroy()
    }
}

@Serializable
data class AgentInfo(
    val id: String,
    val name: String,
    val protocol: String,
    val directory: String?,
    val connectionParams: Map<String, String>?
)
