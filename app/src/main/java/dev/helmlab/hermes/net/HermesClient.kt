package dev.helmlab.hermes.net

import dev.helmlab.hermes.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HermesException(message: String, val status: Int = 0) : Exception(message)

/**
 * Thin client over the Hermes OpenAI-compatible API server + native Runs/Sessions REST surface.
 * Runs entirely against the gateway that Hermes exposes on Termux (default 127.0.0.1:8642).
 */
class HermesClient(
    @Volatile var baseUrl: String = "http://127.0.0.1:8642",
    @Volatile var apiKey: String = ""
) {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // streaming
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun normalizedBase() = baseUrl.trimEnd('/')

    private fun newRequest(path: String, method: String = "GET", body: String? = null): Request {
        val b = Request.Builder().url(normalizedBase() + path)
        if (apiKey.isNotBlank()) b.header("Authorization", "Bearer $apiKey")
        when (method) {
            "GET" -> b.get()
            "DELETE" -> b.delete()
            else -> b.method(method, (body ?: "{}").toRequestBody(JSON))
        }
        return b.build()
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw HermesException(extractError(text, resp.code), resp.code)
            }
            text
        }
    }

    private fun extractError(body: String, code: Int): String = try {
        val el = json.parseToJsonElement(body).jsonObject
        el["error"]?.let { e ->
            val o = e.jsonObject
            o["message"]?.jsonPrimitive?.content ?: e.jsonPrimitive.content
        } ?: body.take(300).ifBlank { "HTTP $code" }
    } catch (_: Exception) {
        body.take(300).ifBlank { "HTTP $code" }
    }

    private suspend inline fun <reified T> get(path: String): T =
        json.decodeFromString(execute(newRequest(path)))

    private suspend inline fun <reified T> post(path: String, payload: Any): T =
        json.decodeFromString(execute(newRequest(path, "POST", json.encodeToString(payload))))

    /* ---------------- Discovery ---------------- */

    suspend fun health(): HealthStatus = get("/v1/health")

    suspend fun models(): List<String> = get<ModelList>("/v1/models").data.map { it.id }

    suspend fun skills(): List<SkillInfo> = get("/v1/skills")

    suspend fun toolsets(): List<ToolsetInfo> = get("/v1/toolsets")

    suspend fun capabilitiesRaw(): String = execute(newRequest("/v1/capabilities"))

    suspend fun sessions(limit: Int = 50): List<SessionSummary> =
        get<SessionList>("/api/sessions?limit=$limit").data

    suspend fun deleteSession(id: String) {
        execute(newRequest("/api/sessions/$id", "DELETE"))
    }

    suspend fun sessionMessages(id: String): String =
        execute(newRequest("/api/sessions/$id/messages?inline_images=false"))

    /* ---------------- Chat ---------------- */

    suspend fun chatOnce(
        messages: List<ChatMessage>,
        sessionId: String?,
        model: String?,
        provider: String?,
        instructions: String?
    ): ChatResponse {
        val sys = if (instructions.isNullOrBlank()) null else ChatMessage("system", instructions)
        val body = buildJsonObject {
            put("model", model ?: "hermes-agent")
            if (!provider.isNullOrBlank()) put("provider", provider)
            put("stream", false)
            putJsonArray("messages") {
                sys?.let { addMessage(it) }
                messages.forEach { addMessage(it) }
            }
        }
        val req = Request.Builder()
            .url(normalizedBase() + "/v1/chat/completions")
            .header("Content-Type", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .apply { sessionId?.let { header("X-Hermes-Session-Id", it) } }
            .post(body.toString().toRequestBody(JSON))
            .build()
        return json.decodeFromString(execute(req))
    }

    private fun kotlinx.serialization.json.JsonArrayBuilder.addMessage(m: ChatMessage) {
        add(buildJsonObject {
            put("role", m.role)
            if (m.imageUrl != null) {
                putJsonObject("content") {
                    addJsonObject("type") { put("type", "text"); put("text", m.content) }
                    addJsonObject("image_url") { put("url", m.imageUrl.url) }
                }
            } else {
                put("content", m.content)
            }
        })
    }

    /* ---------------- Runs (native, tool events) ---------------- */

    suspend fun startRun(input: String, sessionId: String?): RunCreated {
        val payload = RunCreateRequest(input = input, sessionId = sessionId)
        return post("/v1/runs", payload)
    }

    suspend fun runStatus(runId: String): RunStatus = get("/v1/runs/$runId")

    suspend fun stopRun(runId: String): String =
        execute(newRequest("/v1/runs/$runId/stop", "POST", "{}"))

    /**
     * Streams run lifecycle events (tool.started / tool.completed / message.delta / run.completed).
     * Returns when the terminal event arrives or [onStop] signals cancellation.
     */
    fun streamRun(runId: String, onEvent: (String, JsonObject) -> Unit): Call {
        val req = Request.Builder()
            .url(normalizedBase() + "/v1/runs/$runId/events")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .get()
            .build()
        val call = client.newCall(req)
        return call
    }

    /* ---------------- SSE chat (session chat stream) ---------------- */

    fun streamChat(sessionId: String, input: String, onEvent: (String, JsonObject) -> Unit): Call {
        val body = buildJsonObject { put("input", input) }.toString()
        val req = Request.Builder()
            .url(normalizedBase() + "/api/sessions/$sessionId/chat/stream")
            .header("Content-Type", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .post(body.toRequestBody(JSON))
            .build()
        return client.newCall(req)
    }

    companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        /** Parses one raw SSE line into (eventName, payloadJson) or null for keepalive/blank. */
        fun parseSseLine(line: String): Pair<String, JsonObject>? {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith(":")) return null
            if (!t.startsWith("data:")) return null
            val raw = t.removePrefix("data:").trim()
            if (raw.isEmpty()) return null
            return try {
                val el = Json.parseToJsonElement(raw)
                if (el is JsonObject) {
                    val ev = el["event"]?.jsonPrimitive?.content ?: el["type"]?.jsonPrimitive?.content ?: "message"
                    ev to el
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}