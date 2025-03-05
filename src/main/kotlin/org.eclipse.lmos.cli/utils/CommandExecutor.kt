package org.eclipse.lmos.cli.utils

import java.io.BufferedReader
import java.util.concurrent.TimeUnit


fun executeCommand(command: Array<String>, wait: Boolean = true): String {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    var output = ""
    if(wait) {
        output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        process.waitFor()
    }
    return output
}

fun executeCommandStreaming(command: Array<String>, timeoutSeconds: Long, logs: MutableList<String>) {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    val reader = process.inputStream.bufferedReader()

    reader.useLines { lines ->
        for (line in lines) {
            println("Streaming log: $line")
            logs.add(line)
        }
    }
    process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
}