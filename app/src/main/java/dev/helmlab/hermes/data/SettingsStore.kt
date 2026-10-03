package dev.helmlab.hermes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("hermes_settings")

data class HermesSettings(
    val baseUrl: String = "http://127.0.0.1:8642",
    val apiKey: String = "",
    val model: String = "hermes-agent",
    val provider: String = "",
    val reasoningEffort: String = "medium",
    val systemPrompt: String = "",
    val streaming: Boolean = true,
    val useRunsApi: Boolean = true,
    val sessionId: String = "",
    val themeMode: String = "system",
    val accent: String = "#6C5CE7",
    val glassBlur: Boolean = true,
    val animations: Boolean = true,
    val fontScale: Float = 1.0f,
    val showToolEvents: Boolean = true,
    val sendOnEnter: Boolean = false,
    val hapticFeedback: Boolean = true,
    val maxContextMessages: Int = 40,
    val temperatureNote: String = ""
)

class SettingsStore(private val context: Context) {
    private object K {
        val baseUrl = stringPreferencesKey("base_url")
        val apiKey = stringPreferencesKey("api_key")
        val model = stringPreferencesKey("model")
        val provider = stringPreferencesKey("provider")
        val reasoning = stringPreferencesKey("reasoning_effort")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val streaming = booleanPreferencesKey("streaming")
        val useRuns = booleanPreferencesKey("use_runs")
        val sessionId = stringPreferencesKey("session_id")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val glass = booleanPreferencesKey("glass")
        val animations = booleanPreferencesKey("animations")
        val fontScale = intPreferencesKey("font_scale")
        val showTools = booleanPreferencesKey("show_tool_events")
        val sendOnEnter = booleanPreferencesKey("send_on_enter")
        val haptic = booleanPreferencesKey("haptic")
        val maxCtx = intPreferencesKey("max_context_messages")
    }

    val flow: Flow<HermesSettings> = context.dataStore.data.map { p ->
        HermesSettings(
            baseUrl = p[K.baseUrl] ?: "http://127.0.0.1:8642",
            apiKey = p[K.apiKey] ?: "",
            model = p[K.model] ?: "hermes-agent",
            provider = p[K.provider] ?: "",
            reasoningEffort = p[K.reasoning] ?: "medium",
            systemPrompt = p[K.systemPrompt] ?: "",
            streaming = p[K.streaming] ?: true,
            useRunsApi = p[K.useRuns] ?: true,
            sessionId = p[K.sessionId] ?: "",
            themeMode = p[K.themeMode] ?: "system",
            accent = p[K.accent] ?: "#6C5CE7",
            glassBlur = p[K.glass] ?: true,
            animations = p[K.animations] ?: true,
            fontScale = (p[K.fontScale] ?: 100) / 100f,
            showToolEvents = p[K.showTools] ?: true,
            sendOnEnter = p[K.sendOnEnter] ?: false,
            hapticFeedback = p[K.haptic] ?: true,
            maxContextMessages = p[K.maxCtx] ?: 40
        )
    }

    suspend fun set(key: String, value: String) {
        context.dataStore.edit { p ->
            when (key) {
                "base_url" -> p[K.baseUrl] = value
                "api_key" -> p[K.apiKey] = value
                "model" -> p[K.model] = value
                "provider" -> p[K.provider] = value
                "reasoning_effort" -> p[K.reasoning] = value
                "system_prompt" -> p[K.systemPrompt] = value
                "session_id" -> p[K.sessionId] = value
                "theme_mode" -> p[K.themeMode] = value
                "accent" -> p[K.accent] = value
            }
        }
    }

    suspend fun setBool(key: String, value: Boolean) {
        context.dataStore.edit { p ->
            when (key) {
                "streaming" -> p[K.streaming] = value
                "use_runs" -> p[K.useRuns] = value
                "glass" -> p[K.glass] = value
                "animations" -> p[K.animations] = value
                "show_tool_events" -> p[K.showTools] = value
                "send_on_enter" -> p[K.sendOnEnter] = value
                "haptic" -> p[K.haptic] = value
            }
        }
    }

    suspend fun setInt(key: String, value: Int) {
        context.dataStore.edit { p ->
            when (key) {
                "font_scale" -> p[K.fontScale] = value
                "max_context_messages" -> p[K.maxCtx] = value
            }
        }
    }
}