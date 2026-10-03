package dev.helmlab.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.helmlab.hermes.data.HermesSettings
import dev.helmlab.hermes.ui.HermesViewModel
import dev.helmlab.hermes.ui.components.GlassCard
import dev.helmlab.hermes.ui.components.SectionTitle
import dev.helmlab.hermes.ui.components.StatPill
import dev.helmlab.hermes.ui.theme.mix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val disc by vm.discovery.collectAsState()
    var local by remember { mutableStateOf(s) }
    var showKey by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // keep local edits synced with external refreshes
    LaunchedEffect(s.baseUrl, s.apiKey, s.model, s.provider) {
        if (local != s) local = local.copy(
            baseUrl = s.baseUrl, apiKey = s.apiKey, model = s.model, provider = s.provider
        )
    }

    fun apply() {
        vm.updateSettings(local)
        vm.discover()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات هرمس", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            /* ---------- connection status ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("وضعیت اتصال", Icons.Default.CloudDone)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill(
                            if (disc.connected) "متصل" else "قطع",
                            "گیت‌وی",
                            if (disc.connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        StatPill(disc.health?.version ?: "—", "نسخهٔ هرمس")
                        StatPill(disc.models.size.toString(), "مدل")
                        StatPill(disc.skills.size.toString(), "مهارت")
                    }
                    if (disc.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            disc.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(onClick = { apply() }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ذخیره و بررسی مجدد")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { vm.discover() }) { Text("فقط تست اتصال") }
                    }
                }
            }

            /* ---------- connection ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("اتصال به گیت‌وی", Icons.Default.Lan)
                    Spacer(Modifier.height(12.dp))
                    Field(
                        label = "آدرس پایه (Base URL)",
                        value = local.baseUrl,
                        onChange = { local = local.copy(baseUrl = it) },
                        keyboard = KeyboardType.Uri,
                        placeholder = "http://127.0.0.1:8642"
                    )
                    Spacer(Modifier.height(10.dp))
                    Field(
                        label = "کلید API (Bearer)",
                        value = local.apiKey,
                        onChange = { local = local.copy(apiKey = it) },
                        keyboard = KeyboardType.Password,
                        password = !showKey,
                        trailing = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    if (showKey) "پنهان کردن" else "نمایش",
                                    Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "اگر گیت‌وی روی ۱۲۷.۰.۰.۱ اجرا می‌شود همین آدرس درست است. برای دسترسی از دستگاه دیگر، پورت را در ترموکس باز کنید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            /* ---------- model & provider ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("مدل و پرووایدر", Icons.Default.Psychology)
                    Spacer(Modifier.height(12.dp))
                    Field(
                        label = "مدل",
                        value = local.model,
                        onChange = { local = local.copy(model = it) },
                        placeholder = "hermes-agent",
                        trailing = {
                            if (disc.models.isNotEmpty()) {
                                AssistChip(
                                    onClick = {
                                        ModalBottomSheet(onDismissRequest = { }, sheetState = sheetState) {
                                            Column(Modifier.padding(16.dp)) {
                                                SectionTitle("مدل‌های در دسترس")
                                                Spacer(Modifier.height(8.dp))
                                                disc.models.forEach { m ->
                                                    ListItem(
                                                        headlineContent = { Text(m, style = MaterialTheme.typography.bodyMedium) },
                                                        trailingContent = {
                                                            IconButton(onClick = { local = local.copy(model = m) }) {
                                                                Icon(Icons.Default.Check, "انتخاب")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("${disc.models.size} مدل") }
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Field(
                        label = "پرووایدر (اختیاری)",
                        value = local.provider,
                        onChange = { local = local.copy(provider = it) },
                        placeholder = "openrouter / anthropic / openai …"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("سطح استدلال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    SegmentedRow(
                        options = listOf("none", "low", "medium", "high"),
                        selected = local.reasoningEffort,
                        onSelect = { local = local.copy(reasoningEffort = it) }
                    )
                }
            }

            /* ---------- agent behaviour ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("رفتار ایجنت", Icons.Default.SettingsSuggest)
                    Spacer(Modifier.height(12.dp))
                    Field(
                        label = "پرامپت سیستم (اختیاری)",
                        value = local.systemPrompt,
                        onChange = { local = local.copy(systemPrompt = it) },
                        minLines = 3,
                        placeholder = "مثلاً: همیشه پاسخ‌ها را کوتاه و به فارسی بده"
                    )
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        "پاسخ جریانی (Streaming)", "نمایش تدریجی توکن‌ها به‌جای انتظار کامل",
                        s.streaming
                    ) { vm.saveBool("streaming", it); local = local.copy(streaming = it) }
                    SwitchRow(
                        "استفاده از Runs API", "دریدادن رویداد ابزارها به‌صورت زنده (پیشنهاد می‌شود)",
                        s.useRunsApi
                    ) { vm.saveBool("use_runs", it); local = local.copy(useRunsApi = it) }
                    SwitchRow(
                        "نمایش رویداد ابزارها", "نمایش هر ابزاری که هرمس اجرا می‌کند",
                        s.showToolEvents
                    ) { vm.saveBool("show_tool_events", it); local = local.copy(showToolEvents = it) }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "حداکثر پیام‌های زمینه: ${local.maxContextMessages}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = local.maxContextMessages.toFloat(),
                        onValueChange = { local = local.copy(maxContextMessages = it.toInt()) },
                        onValueChangeFinished = { vm.saveInt("max_context_messages", local.maxContextMessages) },
                        valueRange = 4f..120f,
                        steps = 22
                    )
                }
            }

            /* ---------- tools & skills ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("ابزارها و مهارت‌ها", Icons.Default.Extension)
                    Spacer(Modifier.height(12.dp))
                    if (disc.toolsets.isEmpty() && disc.skills.isEmpty()) {
                        Text(
                            "پس از اتصال، ابزارها و مهارت‌های فعال هرمس اینجا فهرست می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        disc.toolsets.forEach { t ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (t.enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null, Modifier.size(16.dp),
                                        tint = if (t.enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        t.label.ifBlank { t.name },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${t.tools.size} ابزار",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (t.tools.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        t.tools.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight
                                    )
                                }
                            }
                        }
                        if (disc.skills.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Spacer(Modifier.height(8.dp))
                            SectionTitle("مهارت‌ها (${disc.skills.size})", Icons.Default.AutoAwesome)
                            Spacer(Modifier.height(6.dp))
                            disc.skills.take(40).forEach { sk ->
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Text(sk.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    if (sk.description.isNotBlank())
                                        Text(
                                            sk.description.take(140),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                            }
                        }
                    }
                }
            }

            /* ---------- appearance ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("ظاهر و تم", Icons.Default.Palette)
                    Spacer(Modifier.height(12.dp))
                    Text("حالت نمایش", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    SegmentedRow(
                        options = listOf("system", "light", "dark"),
                        selected = local.themeMode,
                        labels = mapOf("system" to "سیستم", "light" to "روشن", "dark" to "تاریک"),
                        onSelect = { vm.saveString("theme_mode", it); local = local.copy(themeMode = it) }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("رنگ تأکیدی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val palette = listOf(
                        "#6C5CE7", "#00B894", "#0984E3", "#E17055",
                        "#D63031", "#E84393", "#F39C12", "#2D3436"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        palette.take(4).forEach { hex ->
                            AccentSwatch(hex, local.accent == hex) { vm.saveString("accent", hex); local = local.copy(accent = hex) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        palette.drop(4).forEach { hex ->
                            AccentSwatch(hex, local.accent == hex) { vm.saveString("accent", hex); local = local.copy(accent = hex) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("اندازهٔ فونت: ${(local.fontScale * 100).toInt()}٪", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = local.fontScale,
                        onValueChange = { vm.saveInt("font_scale", (it * 100).toInt()); local = local.copy(fontScale = it) },
                        valueRange = 0.85f..1.35f
                    )
                    SwitchRow("جلوهٔ شیشه‌ای", "ظاهر نیمه‌شفاف و مدرن کارت‌ها", s.glassBlur) {
                        vm.saveBool("glass", it); local = local.copy(glassBlur = it)
                    }
                    SwitchRow("انیمیشن‌ها", "حرکت نرم در گذارها", s.animations) {
                        vm.saveBool("animations", it); local = local.copy(animations = it)
                    }
                    SwitchRow("لرزش لمسی", "بازخورد لمسی هنگام ارسال", s.hapticFeedback) {
                        vm.saveBool("haptic", it); local = local.copy(hapticFeedback = it)
                    }
                }
            }

            /* ---------- sessions ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("مدیریت جلسات", Icons.Default.Forum)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (s.sessionId.isBlank()) "در حال حاضر جلسهٔ فعالی ثبت نشده است."
                        else "جلسهٔ فعال: ${s.sessionId.take(28)}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(onClick = { vm.newSession() }) {
                            Icon(Icons.Default.AddComment, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جلسهٔ جدید")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { vm.loadSessions() }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تازه‌سازی")
                        }
                    }
                }
            }

            /* ---------- about ---------- */
            item {
                GlassCard(glass = s.glassBlur) {
                    SectionTitle("درباره", Icons.Default.Info)
                    Spacer(Modifier.height(10.dp))
                    InfoRow("نسخهٔ برنامه", "۱٫۰٫۰")
                    InfoRow("محدودهٔ نشست", "/root/.hermes")
                    InfoRow("پروتکل", "OpenAI-compatible + Runs REST + SSE")
                    InfoRow("زبان رابط", "فارسی (راست‌به‌چپ)")
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    minLines: Int = 1,
    trailing: @Composable (() -> Unit)? = null
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = minLines == 1,
            minLines = minLines,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SegmentedRow(
    options: List<String>,
    selected: String,
    labels: Map<String, String> = emptyMap(),
    onSelect: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (active) Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        ) else Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable { onSelect(opt) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels[opt] ?: opt,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val c = remember(hex) {
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
    }
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(c, c.mix(Color.White, 0.35f))))
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.White)
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}