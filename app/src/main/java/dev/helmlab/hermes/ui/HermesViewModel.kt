package dev.helmlab.hermes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.helmlab.hermes.data.*
import dev.helmlab.hermes.net.HermesClient
import dev.helmlab.hermes.net.HermesException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

data class DiscoveryState(
    val connected: Boolean = false,
    val health: HealthStatus? = null,
    val models: List<String> = emptyList(),
    val skills: List<SkillInfo> = emptyList(),
    val toolsets: List<ToolsetInfo> = emptyList(),
    val error: String? = null,
    val busy: Boolean = false
)

class HermesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val client = HermesClient()

    val settings: StateFlow<HermesSettings> = MutableStateFlow(HermesSettings())

    private val _items = MutableStateFlow<List<ChatItem>>(emptyList())
    val items: StateFlow<List<ChatItem>> = _items.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _discovery = MutableStateFlow(DiscoveryState())
    val discovery: StateFlow<DiscoveryState> = _discovery.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _sessionsBusy = MutableStateFlow(false)
    val sessionsBusy: StateFlow<Boolean> = _sessionsBusy.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val currentSessionId: String? get() = settings.value.sessionId.ifBlank { null }

    private var activeRun: String? = null
    private var activeCall: Call? = null
    private val seq = AtomicLong(0)

    init {
        viewModelScope.launch {
            store.flow.collect { s ->
                val changed = client.baseUrl != s.baseUrl || client.apiKey != s.apiKey
                settings.value = s
                client.baseUrl = s.baseUrl
                client.apiKey = s.apiKey
                if (changed) discover()
            }
        }
        viewModelScope.launch { discover() }
    }

    fun clearToast() { _toast.value = null }

    private fun toast(m: String) { _toast.value = m }

    /* ------------- settings ------------- */

    fun saveString(key: String, v: String) = viewModelScope.launch { store.set(key, v) }
    fun saveBool(key: String, v: Boolean) = viewModelScope.launch { store.setBool(key, v) }
    fun saveInt(key: String, v: Int) = viewModelScope.launch { store.setInt(key, v) }

    fun updateSettings(s: HermesSettings) {
        saveString("base_url", s.baseUrl)
        saveString("api_key", s.apiKey)
        saveString("model", s.model)
        saveString("provider", s.provider)
        saveString("reasoning_effort", s.reasoningEffort)
        saveString("system_prompt", s.systemPrompt)
        saveString("theme_mode", s.themeMode)
        saveString("accent", s.accent)
        saveBool("streaming", s.streaming)
        saveBool("use_runs", s.useRunsApi)
        saveBool("glass", s.glassBlur)
        saveBool("animations", s.animations)
        saveBool("show_tool_events", s.showToolEvents)
        saveBool("send_on_enter", s.sendOnEnter)
        saveBool("haptic", s.hapticFeedback)
        saveInt("font_scale", (s.fontScale * 100).toInt())
        saveInt("max_context_messages", s.maxContextMessages)
    }

    /* ------------- discovery ------------- */

    fun discover() {
        viewModelScope.launch {
            _discovery.value = _discovery.value.copy(busy = true, error = null)
            try {
                val h = client.health()
                val models = runCatching { client.models() }.getOrDefault(emptyList())
                val skills = runCatching { client.skills() }.getOrDefault(emptyList())
                val toolsets = runCatching { client.toolsets() }.getOrDefault(emptyList())
                _discovery.value = DiscoveryState(
                    connected = h.status.equals("ok", true),
                    health = h, models = models, skills = skills, toolsets = toolsets,
                    busy = false
                )
                loadSessions()
            } catch (e: Exception) {
                _discovery.value = DiscoveryState(error = friendly(e), busy = false)
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _sessionsBusy.value = true
            _sessions.value = runCatching { client.sessions(50) }.getOrDefault(emptyList())
            _sessionsBusy.value = false
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            runCatching { client.deleteSession(id) }
                .onSuccess { toast("جلسه حذف شد"); loadSessions() }
                .onFailure { toast(friendly(it)) }
        }
    }

    /* ------------- session controls ------------- */

    fun newSession() {
        saveString("session_id", "")
        _items.value = emptyList()
        toast("جلسه جدید آغاز شد")
    }

    fun attachSession(id: String) {
        saveString("session_id", id)
        toast("به جلسه متصل شد")
    }

    fun clearMessages() { _items.value = emptyList() }

    /* ------------- chat ------------- */

    fun send(text: String, imageDataUrl: String? = null) {
        val s = settings.value
        if (text.isBlank() && imageDataUrl == null) return
        if (_busy.value) return

        val userItem = ChatItem(
            id = seq.incrementAndGet(),
            role = "user",
            text = text.ifBlank { "تصویر" },
            imageDataUrl = imageDataUrl
        )
        _items.value = _items.value + userItem

        val botId = seq.incrementAndGet()
        _items.value = _items.value + ChatItem(botId, "assistant", "", pending = true)
        _busy.value = true

        viewModelScope.launch {
            try {
                if (s.useRunsApi && s.streaming) streamViaRuns(botId, text, imageDataUrl)
                else oneShot(botId, s, imageDataUrl)
            } catch (e: Exception) {
                patch(botId) { it.copy(pending = false, error = true, text = friendly(e)) }
            } finally {
                _busy.value = false
                loadSessions()
            }
        }
    }

    private suspend fun oneShot(botId: Long, s: HermesSettings, imageDataUrl: String?) {
        val history = _items.value
            .filter { !it.pending && !it.error && it.role in listOf("user", "assistant") }
            .takeLast(s.maxContextMessages)
            .map { item ->
                if (item.imageDataUrl != null && item.role == "user")
                    ChatMessage("user", item.text, ImageUrl(item.imageDataUrl))
                else ChatMessage(item.role, item.text)
            }
        // ensure latest user image is present
        if (imageDataUrl != null) {
            history.removeAll { it.role == "user" && it.imageUrl != null }
            history.add(ChatMessage("user", _items.value.last().text, ImageUrl(imageDataUrl)))
        }

        val resp = client.chatOnce(
            messages = history,
            sessionId = currentSessionId,
            model = s.model.ifBlank { null },
            provider = s.provider.ifBlank { null },
            instructions = s.systemPrompt.ifBlank { null }
        )
        val text = resp.choices.firstOrNull()?.message?.content.orEmpty()
        patch(botId) { it.copy(pending = false, text = text) }
        if (resp.sessionId != null && currentSessionId == null) {
            saveString("session_id", resp.sessionId)
        }
    }

    private suspend fun streamViaRuns(botId: Long, text: String, imageDataUrl: String?) {
        val input = if (imageDataUrl != null) {
            "$text\n\n[image attachment available in prior turns]"
        } else text

        val created = client.startRun(input, currentSessionId)
        activeRun = created.runId
        patch(botId) { it.copy(runId = created.runId) }

        if (!created.sessionId.isNullOrBlank() && currentSessionId == null) {
            saveString("session_id", created.sessionId)
        }

        val call = client.streamRun(created.runId) { _, _ -> }
        activeCall = call

        withContext(Dispatchers.IO) {
            call.execute().use { resp ->
                if (!resp.isSuccessful) throw HermesException("SSE ${resp.code}")
                val source = resp.body?.source() ?: throw IOException("بدنه‌ی خالی")
                var eventName = ""
                val buffer = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8LineStrict()
                    when {
                        line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            buffer.append(line.removePrefix("data:").trim()).append("\n")
                        }
                        line.isBlank() -> {
                            if (buffer.isNotBlank()) {
                                dispatch(botId, eventName, buffer.toString())
                            }
                            eventName = ""
                            buffer.clear()
                        }
                    }
                }
                if (buffer.isNotBlank()) dispatch(botId, eventName, buffer.toString())
            }
        }
    }

    private fun dispatch(botId: Long, event: String, raw: String) {
        val obj = try { Json.parseToJsonElement(raw).jsonObject } catch (_: Exception) { return }
        when (event) {
            "tool.started" -> {
                val name = obj.str("tool")
                val preview = obj.str("preview")
                patch(botId) {
                    it.copy(toolEvents = it.toolEvents + ToolEventLine(name, preview))
                }
            }
            "tool.completed", "tool.failed" -> {
                val name = obj.str("tool")
                val isErr = obj["error"]?.jsonPrimitive?.contentOrNull == "true" || event == "tool.failed"
                val preview = obj.str("preview")
                val dur = obj["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                patch(botId) {
                    val idx = it.toolEvents.indexOfLast { e -> e.name == name }
                    val updated = it.toolEvents.toMutableList()
                    if (idx >= 0) updated[idx] = ToolEventLine(name, preview, isErr, dur)
                    else updated.add(ToolEventLine(name, preview, isErr, dur))
                    it.copy(toolEvents = updated)
                }
            }
            "message.delta", "assistant.delta", "token" -> {
                val d = obj.str("text").ifBlank { obj.str("delta").ifBlank { obj.str("content") } }
                if (d.isNotBlank()) patch(botId) { it.copy(text = it.text + d) }
            }
            "message.interim", "assistant.commentary" -> {
                val t = obj.str("text")
                if (t.isNotBlank()) patch(botId) { it.copy(text = it.text + t) }
            }
            "run.completed" -> {
                patch(botId) {
                    val out = obj.str("output")
                    it.copy(pending = false, text = out.ifBlank { it.text })
                }
            }
            "run.failed" -> {
                patch(botId) { it.copy(pending = false, error = true, text = obj.str("error").ifBlank { "اجرای درخواست ناموفق بود" }) }
            }
            "run.cancelled", "run.interrupted" -> {
                patch(botId) { it.copy(pending = false, text = it.text.ifBlank { "درخواست لغو شد" }) }
            }
        }
    }

    private fun patch(id: Long, block: (ChatItem) -> ChatItem) {
        _items.value = _items.value.map { if (it.id == id) block(it) else it }
    }

    fun stop() {
        val run = activeRun
        activeCall?.cancel()
        activeCall = null
        if (run != null) {
            viewModelScope.launch { runCatching { client.stopRun(run) } }
        }
        _busy.value = false
    }

    fun retryLast() {
        val lastUser = _items.value.lastOrNull { it.role == "user" } ?: return
        _items.value = _items.value.dropLastWhile { it.role == "assistant" } + lastUser
        val prompt = lastUser.text
        _items.value = _items.value.dropLast(1)
        send(prompt, lastUser.imageDataUrl)
    }

    fun friendly(e: Throwable): String = when (e) {
        is HermesException -> e.message ?: "خطای سرور (${e.status})"
        is java.net.ConnectException -> "اتصال به هرمس برقرار نشد. آیا گیت‌وی روی ترموکس اجرا شده و آدرس درست است؟"
        is java.net.SocketTimeoutException -> "پاسخی از سرور نرسید (اتمام مهلت)."
        is IOException -> "خطای شبکه: ${e.message}"
        else -> e.message ?: "خطای ناشناخته"
    }

    override fun onCleared() {
        super.onCleared()
        activeCall?.cancel()
    }
}

private fun JsonObject.str(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: ""