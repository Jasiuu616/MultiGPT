package com.matrix.multigpt.data.dto.bedrock

import kotlinx.serialization.Serializable

@Serializable
data class BedrockRequest(
    val inputText: String? = null,
    val textGenerationConfig: TextGenerationConfig? = null,
    val messages: List<BedrockMessage>? = null,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val stop_sequences: List<String>? = null,
    val anthropic_version: String? = null,
    val system: String? = null
)

@Serializable
data class BedrockMessage(
    val role: String,
    val content: String
)

@Serializable
data class TextGenerationConfig(
    val maxTokenCount: Int? = null,
    val stopSequences: List<String>? = null,
    val temperature: Double? = null,
    val topP: Double? = null
)
