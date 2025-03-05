package org.eclipse.lmos.cli.factory

import org.eclipse.lmos.cli.credential.CredentialManagerType
import org.eclipse.lmos.cli.arc.ArcMacOSAgentManager
import org.eclipse.lmos.cli.agent.AgentManager

class AgentManagerFactory {
    fun agentManager(): AgentManager {
        val os = getOS()
        return when(os) {
            CredentialManagerType.MAC -> ArcMacOSAgentManager()
            CredentialManagerType.WIN -> TODO()
            CredentialManagerType.LINUX -> ArcMacOSAgentManager()
        }
    }

}