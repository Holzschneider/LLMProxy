package io.github.stream29.proxy.client

import io.github.stream29.proxy.server.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("DashScope")
data class DashScopeConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long"),
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@Serializable
@SerialName("DeepSeek")
data class DeepSeekConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("deepseek-chat", "deepseek-reasoner"),
) : ApiProvider {
    val delegate = OpenAiConfig(
        baseUrl = "https://api.deepseek.com/",
        apiKey = apiKey,
        modelList = modelList
    )

    override suspend fun getModelNameList(): List<String> = modelList

    override suspend fun generateLStream(request: LChatCompletionRequest): Flow<LChatCompletionResponseChunk> {
        return delegate.generateLStream(
            request.copy(
                messages = request.messages.mergeBy(
                    selector = { it.role },
                    merger = { a, b -> a.copy(content = a.content + b.content) }
                )
            )
        )
    }

    override suspend fun generateOStream(request: OChatRequest): Flow<OChatResponseChunk> {
        return delegate.generateOStream(
            request.copy(
                messages = request.messages.mergeBy(
                    selector = { it.role },
                    merger = { a, b -> a.copy(content = a.content + b.content) }
                )
            )
        )
    }

    override fun close() {
        delegate.close()
    }

}

@Suppress("unused")
@Serializable
@SerialName("Mistral")
data class MistralConfig(
    val apiKey: String,
    val modelList: List<String> = listOf("codestral", "mistral-large"),
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://api.mistral.ai/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@Serializable
@SerialName("SiliconFlow")
data class SiliconFlowConfig(
    val apiKey: String,
    val modelList: List<String>,
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://api.siliconflow.cn/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@Serializable
@SerialName("Gemini")
data class GeminiConfig(
    val apiKey: String,
    val modelList: List<String>,
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@Serializable
@SerialName("Claude")
data class ClaudeConfig(
    val apiKey: String,
    val modelList: List<String>,
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://api.anthropic.com/v1/",
    apiKey = apiKey,
    modelList = modelList
)

@Suppress("unused")
@Serializable
@SerialName("OpenRouter")
data class OpenRouterConfig(
    val apiKey: String,
    val modelList: List<String>,
) : ApiProvider by OpenAiConfig(
    baseUrl = "https://openrouter.ai/api/v1",
    apiKey = apiKey,
    modelList = modelList
)