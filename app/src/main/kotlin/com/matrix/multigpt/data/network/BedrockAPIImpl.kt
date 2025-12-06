package com.matrix.multigpt.data.network

import com.matrix.multigpt.data.dto.bedrock.BedrockError
import com.matrix.multigpt.data.dto.bedrock.BedrockMessage
import com.matrix.multigpt.data.dto.bedrock.BedrockRequest
import com.matrix.multigpt.data.dto.bedrock.BedrockStreamChunk
import com.matrix.multigpt.data.dto.bedrock.TextGenerationConfig
import com.matrix.multigpt.util.AwsSignatureV4
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

class BedrockAPIImpl @Inject constructor(
    private val networkClient: NetworkClient
) : BedrockAPI {

    private var credentials: AwsSignatureV4.AwsCredentials? = null
    private var region: String = "us-east-1"

    override fun setCredentials(credentials: AwsSignatureV4.AwsCredentials?) {
        this.credentials = credentials
    }

    override fun setRegion(region: String) {
        this.region = region
    }

    override fun streamChatMessage(
        messages: List<Pair<String, String>>,
        model: String,
        systemPrompt: String?,
        temperature: Double?,
        maxTokens: Int?,
        topP: Double?
    ): Flow<BedrockStreamChunk> {
        
        return flow {
            try {
                val creds = credentials ?: run {
                    emit(BedrockStreamChunk(error = BedrockError(type = "auth_error", message = "AWS credentials not configured")))
                    return@flow
                }

                val request = createRequest(messages, model, systemPrompt, temperature, maxTokens, topP)
                val requestBody = Json.encodeToJsonElement(request).toString()
                
                val uri = "/model/${model}/invoke-with-response-stream"
                val endpoint = "https://bedrock-runtime.${region}.amazonaws.com${uri}"
                
                // Sign the request
                val headers = mutableMapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/vnd.amazon.eventstream"
                )
                
                val signedRequest = AwsSignatureV4.signRequest(
                    method = "POST",
                    uri = uri,
                    headers = headers,
                    payload = requestBody,
                    credentials = creds,
                    service = "bedrock"
                )

                val builder = HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                    url(endpoint)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                    
                    signedRequest.headers.forEach { (key, value) ->
                        headers { append(key, value) }
                    }
                }

                HttpStatement(builder = builder, client = networkClient()).execute { response ->
                    streamEventsFrom(response, model)
                }
                
            } catch (e: Exception) {
                emit(BedrockStreamChunk(error = BedrockError(type = "network_error", message = e.message ?: "Unknown error")))
            }
        }
    }

    private fun createRequest(
        messages: List<Pair<String, String>>,
        model: String,
        systemPrompt: String?,
        temperature: Double?,
        maxTokens: Int?,
        topP: Double?
    ): BedrockRequest {
        
        return when {
            model.startsWith("anthropic.claude") -> {
                // Anthropic Claude format
                BedrockRequest(
                    messages = messages.map { BedrockMessage(role = it.first, content = it.second) },
                    max_tokens = maxTokens ?: 4096,
                    temperature = temperature,
                    top_p = topP,
                    system = systemPrompt,
                    anthropic_version = "bedrock-2023-05-31"
                )
            }
            model.startsWith("amazon.titan") -> {
                // Amazon Titan format
                val conversationText = buildString {
                    if (!systemPrompt.isNullOrEmpty()) {
                        append("System: $systemPrompt\n\n")
                    }
                    messages.forEach { (role, content) ->
                        val displayRole = when (role) {
                            "user" -> "User"
                            "assistant" -> "Assistant"
                            else -> role.capitalize()
                        }
                        append("$displayRole: $content\n\n")
                    }
                    append("Assistant:")
                }
                
                BedrockRequest(
                    inputText = conversationText,
                    textGenerationConfig = TextGenerationConfig(
                        maxTokenCount = maxTokens ?: 4096,
                        temperature = temperature,
                        topP = topP
                    )
                )
            }
            model.startsWith("ai21.") -> {
                // AI21 format
                val prompt = buildString {
                    if (!systemPrompt.isNullOrEmpty()) {
                        append("$systemPrompt\n\n")
                    }
                    messages.forEach { (role, content) ->
                        val displayRole = when (role) {
                            "user" -> "Human"
                            "assistant" -> "Assistant"
                            else -> role.capitalize()
                        }
                        append("$displayRole: $content\n\n")
                    }
                    append("Assistant:")
                }
                
                BedrockRequest(
                    inputText = prompt,
                    textGenerationConfig = TextGenerationConfig(
                        maxTokenCount = maxTokens ?: 4096,
                        temperature = temperature,
                        topP = topP
                    )
                )
            }
            model.startsWith("cohere.") -> {
                // Cohere format
                val prompt = buildString {
                    if (!systemPrompt.isNullOrEmpty()) {
                        append("$systemPrompt\n\n")
                    }
                    messages.forEach { (role, content) ->
                        append("$content\n")
                    }
                }
                
                BedrockRequest(
                    inputText = prompt,
                    textGenerationConfig = TextGenerationConfig(
                        maxTokenCount = maxTokens ?: 4096,
                        temperature = temperature,
                        topP = topP
                    )
                )
            }
            model.startsWith("meta.llama") -> {
                // Meta Llama format
                val prompt = buildString {
                    if (!systemPrompt.isNullOrEmpty()) {
                        append("<s>[INST] <<SYS>>\n$systemPrompt\n<</SYS>>\n\n")
                    } else {
                        append("<s>[INST] ")
                    }
                    
                    messages.forEachIndexed { index, (role, content) ->
                        when (role) {
                            "user" -> {
                                if (index == 0 && !systemPrompt.isNullOrEmpty()) {
                                    append("$content [/INST]")
                                } else {
                                    append("<s>[INST] $content [/INST]")
                                }
                            }
                            "assistant" -> append(" $content </s>")
                        }
                    }
                    
                    if (messages.lastOrNull()?.first == "user") {
                        // Ready for assistant response
                    } else {
                        append("<s>[INST] ")
                    }
                }
                
                BedrockRequest(
                    inputText = prompt,
                    textGenerationConfig = TextGenerationConfig(
                        maxTokenCount = maxTokens ?: 4096,
                        temperature = temperature,
                        topP = topP
                    )
                )
            }
            else -> {
                // Default format
                BedrockRequest(
                    messages = messages.map { BedrockMessage(role = it.first, content = it.second) },
                    max_tokens = maxTokens ?: 4096,
                    temperature = temperature,
                    top_p = topP,
                    system = systemPrompt
                )
            }
        }
    }

    private suspend inline fun <reified T> FlowCollector<T>.streamEventsFrom(
        response: HttpResponse,
        model: String
    ) {
        val channel: ByteReadChannel = response.body()
        val jsonInstance = Json { ignoreUnknownKeys = true }

        try {
            while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                
                when {
                    line.isEmpty() -> continue
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        
                        try {
                            val chunk: T = jsonInstance.decodeFromString(data)
                            emit(chunk)
                        } catch (e: Exception) {
                            // Try to parse as error or continue
                            continue
                        }
                    }
                    line.startsWith("event:") -> {
                        val event = line.removePrefix("event:").trim()
                        if (event == "completion" || event == "done") break
                    }
                }
            }
        } finally {
            channel.cancel()
        }
    }
}
