package dev.helmlab.hermes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/* ---------- Chat Completions (OpenAI compatible) ---------- */

@Serializable
data class ChatMessage(
    val role: String,
    val content: String = "",
    @SerialName("image_url") val imageUrl: ImageUrl? = null
)

@Serializable
data class ImageUrl(val url: String)

@Serializable
data class ChatRequest(
    val model: String = "hermes-agent",
    val provider: String? = null,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    @SerialName("model_options") val modelOptions: JsonObject? = null
)

@Serializable
data class ChatChoice(val message: ChatMessage, @SerialName("finish_reason") val finishReason: String? = null)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
    @SerialName("cache_read_tokens") val cacheReadTokens: Int = 0
)

@Serializable
data class ChatResponse(
    val id: String = "",
    val model: String = "",
    val choices: List<ChatChoice> = emptyList(),
    val usage: Usage? = null,
    @SerialName("session_id") val sessionId: String? = null
)

/* ---------- Runs API ---------- */

@Serializable
data class RunCreateRequest(
    val input: String,
    @SerialName("session_id") val sessionId: String? = null,
    val instructions: String? = null,
    val model: String? = null,
    val provider: String? = null
)

@Serializable
data class RunCreated(
    @SerialName("run_id") val runId: String,
    val status: String = "started",
    @SerialName("session_id") val sessionId: String? = null
)

@Serializable
data class Runtime(
    val provider: String? = null,
    val model: String? = null,
    @SerialName("route_source") val routeSource: String? = null
)

@Serializable
data class RunStatus(
    @SerialName("run_id") val runId: String,
    val status: String,
    @SerialName("session_id") val sessionId: String? = null,
    val model: String? = null,
    val output: String? = null,
    val usage: Usage? = null,
    val runtime: Runtime? = null,
    val error: String? = null
)

/* ---------- Discovery ---------- */

@Serializable
data class ModelInfo(val id: String, val object: String = "model", @SerialName("owned_by") val owner: String = "hermes")

@Serializable
data class ModelList(val data: List<ModelInfo> = emptyList())

@Serializable
data class SkillInfo(val name: String, val description: String = "", val category: String? = null)

@Serializable
data class ToolsetInfo(
    val name: String,
    val label: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val configured: Boolean = true,
    val tools: List<String> = emptyList()
)

/* ---------- Sessions ---------- */

@Serializable
data class SessionSummary(
    val id: String,
    val source: String = "",
    val model: String? = null,
    val title: String? = null,
    @SerialName("started_at") val startedAt: Double = 0.0,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("tool_call_count") val toolCallCount: Int = 0,
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
    val preview: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false
)

@Serializable
data class SessionList(val data: List<SessionSummary> = emptyList())

@Serializable
data class ChatTurnRequest(
    val input: String,
    val model: String? = null,
    val provider: String? = null,
    val instructions: String? = null
)

/* ---------- Health ---------- */

@Serializable
data class HealthStatus(
    val status: String = "unknown",
    val platform: String? = null,
    val version: String? = null
)

/* ---------- Local chat item ---------- */

data class ChatItem(
    val id: Long,
    val role: String,
    val text: String,
    val pending: Boolean = false,
    val error: Boolean = false,
    val imageDataUrl: String? = null,
    val runId: String? = null,
    val toolEvents: List<ToolEventLine> = emptyList()
)

data class ToolEventLine(val name: String, val preview: String = "", val error: Boolean = false, val seconds: Double = 0.0)

fun JsonElement?.safeText(): String = this?.toString()?.takeIf { it.isNotBlank() } ?: ""