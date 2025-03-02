/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.cli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.eclipse.lmos.arc.agent.client.graphql.GraphQlAgentClient
import org.eclipse.lmos.arc.api.*
import org.slf4j.LoggerFactory

class ArcAgentClientService {
    private val log = LoggerFactory.getLogger(ArcAgentClientService::class.java)

    suspend fun askAgent(
        conversation: Conversation,
        conversationId: String,
        turnId: String,
        agentName: String,
        host: String,
    ): Flow<String> =
        flow {
            createGraphQlAgentClient(host).use { graphQlAgentClient ->


                val agentRequest =
                    AgentRequest(
                        conversationContext =
                            ConversationContext(
                                conversationId = conversationId
                            ),
                        systemContext =
                            conversation.systemContext.contextParams.map { (key, value) ->
                                SystemContextEntry(key, value)
                            }.toList(),
                        userContext =
                            UserContext(
                                userId = conversation.userContext.userId,
                                userToken = conversation.userContext.userToken,
                                profile =
                                    conversation.userContext.contextParams.map { (key, value) ->
                                        ProfileEntry(key, value)
                                    }.toList(),
                            ),
                        messages = conversation.inputContext.messages,
                    )

                try {
                    graphQlAgentClient.callAgent(
                        agentRequest,
                        agentName
                    ).collect { response ->
                        log.info("Agent Response: $response")
                        emit(
                                response.messages[0].content
                            )
                    }
                } catch (e: Exception) {
                    log.error("Error response from ArcAgentClient", e)
                    throw RuntimeException(e.message)
                }
            }
        }.flowOn(Dispatchers.IO)

    private fun createGraphQlAgentClient(host: String): GraphQlAgentClient {
        val agentUrl = "ws://${host}:8080/subscriptions"

        log.info("Creating GraphQlAgentClient with url $agentUrl")
        val graphQlAgentClient = GraphQlAgentClient(agentUrl)
        return graphQlAgentClient
    }
}
