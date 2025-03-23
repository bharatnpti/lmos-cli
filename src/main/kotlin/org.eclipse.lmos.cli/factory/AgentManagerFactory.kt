package org.eclipse.lmos.cli.factory

import org.eclipse.lmos.cli.credential.CredentialManagerType
import org.eclipse.lmos.cli.arc.ArcMacOSAgentManager
import org.eclipse.lmos.cli.agent.AgentManager
import org.eclipse.lmos.cli.arc.ArcWindowsAgentManager

class AgentManagerFactory {
    fun agentManager(): AgentManager {
        val os = getOS()
        return when(os) {
            CredentialManagerType.MAC -> ArcMacOSAgentManager()
            CredentialManagerType.WIN -> ArcWindowsAgentManager()
            CredentialManagerType.LINUX -> TODO()
        }
    }

}