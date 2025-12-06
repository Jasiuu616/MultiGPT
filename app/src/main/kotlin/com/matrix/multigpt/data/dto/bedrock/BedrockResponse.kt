package com.matrix.multigpt.data.dto.bedrock

import kotlinx.serialization.Serializable

@Serializable
data class BedrockResponse(
    val outputText: String? = null,
    val results: List<BedrockResult>? = null,
    val content: List<BedrockContent>? = null,
    val stop_reason: String? = null,
    val usage: BedrockUsage? = null
)

@Serializable
data class BedrockResult(
    val tokenCount: Int? = null,
    val outputText: String? = null,
    val completionReason: String? = null
)

@Serializable
data class BedrockContent(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class BedrockUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null
)

@Serializable
data class BedrockStreamChunk(
    val type: String? = null,
    val delta: BedrockDelta? = null,
    val content_block: BedrockContentBlock? = null,
    val message: BedrockStreamMessage? = null,
    val usage: BedrockUsage? = null,
    val error: BedrockError? = null
)

@Serializable
data class BedrockDelta(
    val type: String? = null,
    val text: String? = null,
    val stop_reason: String? = null
)

@Serializable
data class BedrockContentBlock(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class BedrockStreamMessage(
    val role: String? = null,
    val content: List<BedrockContent>? = null
)

@Serializable
data class BedrockError(
    val type: String? = null,
    val message: String? = null
)
