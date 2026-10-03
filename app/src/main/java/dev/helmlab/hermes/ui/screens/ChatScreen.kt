package dev.helmlab.hermes.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.helmlab.hermes.ui.HermesViewModel
import dev.helmlab.hermes.ui.components.EmptyState
import dev.helmlab.hermes.ui.components.MessageBubble
import dev.helmlab.hermes.ui.components.StatusDot
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: HermesViewModel,
    onOpenSettings: () -> Unit,
    onOpenSessions: () -> Unit
) {
    val s by vm.settings.collectAsState()
    val items by vm.items.collectAsState()
    val busy by vm.busy.collectAsState()
    val disc by vm.discovery.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var draft by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { attachedImage = encodeDataUrl(context.contentResolver, it) } }

    LaunchedEffect(items.size, items.lastOrNull()?.text?.length) {
        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("هرمس", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(8.dp))
                                StatusDot(disc.connected)
                            }
                            Text(
                                if (disc.connected)
                                    "متصل — ${disc.health?.version?.let { "نسخهٔ $it" } ?: "گیت‌وی"}" + if (s.model.isNotBlank()) " · ${s.model}" else ""
                                else "قطع — برای اتصال به ترموکس نیاز به گیت‌وی فعال است",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.newSession(); haptics.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                            Icon(Icons.Default.AddComment, "جلسهٔ جدید")
                        }
                        IconButton(onClick = onOpenSessions) {
                            Icon(Icons.Default.History, "تاریخچهٔ جلسات")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Tune, "تنظیمات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            }
        },
        bottomBar = {
            Composer(
                draft = draft,
                onDraft = { draft = it },
                busy = busy,
                attachedImage = attachedImage,
                clearImage = { attachedImage = null },
                pickImage = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                send = {
                    val text = draft
                    if (s.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.send(text, attachedImage)
                    draft = ""
                    attachedImage = null
                },
                stop = { vm.stop() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad)) {
            AuroraBackground()
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "با هرمس گفت‌وگو کنید",
                    body = "این برنامه مستقیماً به گیت‌وی هرمس روی ترموکس وصل می‌شود؛ هر ابزاری که هرمس در اختیار دارد، اینجا در دسترس است."
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        MessageBubble(item, s.showToolEvents)
                    }
                    item {
                        AnimatedVisibility(items.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = { vm.clearMessages() }) {
                                    Icon(Icons.Default.DeleteSweep, null, Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("پاک کردن گفت‌وگو", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraBackground() {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(cs.primary.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(cs.tertiary.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraft: (String) -> Unit,
    busy: Boolean,
    attachedImage: String?,
    clearImage: () -> Unit,
    pickImage: () -> Unit,
    send: () -> Unit,
    stop: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.surface.copy(alpha = 0.92f))
    ) {
        AnimatedVisibility(attachedImage != null) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("یک تصویر پیوست شده", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = clearImage, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "حذف پیوست", Modifier.size(16.dp))
                }
            }
        }
        Divider(color = cs.outline.copy(alpha = 0.22f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = pickImage) {
                Icon(Icons.Default.AddPhotoAlternate, "پیوست تصویر", tint = cs.onSurfaceVariant)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.weight(1f),
                placeholder = { Text("پیام خود را بنویسید…", color = cs.onSurfaceVariant) },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outline.copy(alpha = 0.4f)
                )
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (busy) listOf(cs.error, cs.error.copy(alpha = 0.7f))
                            else listOf(cs.primary, cs.tertiary)
                        )
                    )
                    .clickable { if (busy) stop() else send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (busy) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    if (busy) "توقف" else "ارسال",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

fun encodeDataUrl(resolver: android.content.ContentResolver, uri: Uri): String? = runCatching {
    resolver.openInputStream(uri)?.use { input ->
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        val bmp = BitmapFactory.decodeStream(input, null, opts) ?: return@use null
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}.getOrNull()