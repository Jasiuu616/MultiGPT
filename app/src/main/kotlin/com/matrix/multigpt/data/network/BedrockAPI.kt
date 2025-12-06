package com.matrix.multigpt.data.network

import com.matrix.multigpt.data.dto.bedrock.BedrockStreamChunk
import com.matrix.multigpt.util.AwsSignatureV4
import kotlinx.coroutines.flow.Flow

interface BedrockAPI {
    fun setCredentials(credentials: AwsSignatureV4.AwsCredentials?)
    fun setRegion(region: String)
    fun streamChatMessage(
        messages: List<Pair<String, String>>,
        model: String,
        systemPrompt: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
        topP: Double? = null
    ): Flow<BedrockStreamChunk>
}
