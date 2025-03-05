package org.eclipse.lmos.cli.commands.config.llm

import org.eclipse.lmos.cli.llm.DefaultLLMConfigManager
import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "list",
    description = ["List all credentials"],
)
class ListLLMConfig : Callable<Int> {
    override fun call(): Int {

        val listLLMConfig = DefaultLLMConfigManager().listLLMConfig()
        if (listLLMConfig.isEmpty()) {
            printError("Configuration for LLM Not found")
        } else {
            printSuccess("Found ${listLLMConfig.size} LLM with the following IDs:")
            listLLMConfig
                .forEach {
                    println("""
                |   ID: $it
            """.trimMargin())
                }
        }
        return 0
    }
}


fun printlnHeader(s: String) {
    CommandLine.Help.Ansi.AUTO.string(
        "@|blue $s |@"
    ).also(::println)
}

fun promptUserInput(s: String) {
    CommandLine.Help.Ansi.AUTO.string(
        "@|yellow $s |@"
    ).also(::print)
}

fun printSuccess(s: String) {
    CommandLine.Help.Ansi.AUTO.string(
        "@|bold,green $s |@"
    ).also(::println)
}

fun printConvOutput(role: String, message: String) {
    CommandLine.Help.Ansi.AUTO.string(
        "@|bold,cyan $role |@: $message"
    ).also(::println)
}

fun printError(s: String) {
    CommandLine.Help.Ansi.AUTO.string(
        "@|bold,red $s |@"
    ).also(::println)
}
