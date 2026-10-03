package dev.helmlab.hermes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.helmlab.hermes.data.SessionSummary
import dev.helmlab.hermes.ui.HermesViewModel
import dev.helmlab.hermes.ui.components.EmptyState
import dev.helmlab.hermes.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(vm: HermesViewModel, onBack: () -> Unit) {
    val sessions by vm.sessions.collectAsState()
    val busy by vm.sessionsBusy.collectAsState()
    val s by vm.settings.collectAsState()

    LaunchedEffect(Unit) { vm.loadSessions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تاریخچهٔ جلسات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadSessions() }) { Icon(Icons.Default.Refresh, "تازه‌سازی") }
                    IconButton(onClick = { vm.newSession() }) { Icon(Icons.Default.AddComment, "جلسهٔ جدید") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                busy && sessions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                sessions.isEmpty() -> EmptyState(
                    Icons.Default.Forum,
                    "جلسه‌ای یافت نشد",
                    "پس از اولین گفت‌وگو، جلسات اینجا نگهداری می‌شوند و می‌توانید به آن‌ها بازگردید."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions, key = { it.id }) { sess ->
                        SessionCard(
                            sess,
                            active = sess.id == s.sessionId,
                            onOpen = { vm.attachSession(sess.id); onBack() },
                            onDelete = { vm.deleteSession(sess.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(sess: SessionSummary, active: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GlassCard(glass = true, modifier = Modifier.clickable { onOpen() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .pillBackground(cs.primary.copy(alpha = if (active) 0.28f else 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (active) Icons.Default.PlayCircle else Icons.Default.ChatBubbleOutline,
                    null,
                    Modifier.size(19.dp),
                    tint = cs.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sess.title?.takeIf { it.isNotBlank() } ?: sess.preview?.take(48)?.ifBlank { "بدون عنوان" } ?: "بدون عنوان",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${sess.message_count} پیام · ${sess.tool_call_count} ابزار · ${sess.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            if (active) {
                Box(
                    Modifier
                        .pillBackground(cs.secondary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("فعال", style = MaterialTheme.typography.labelSmall, color = cs.secondary, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "حذف", tint = cs.error.copy(alpha = 0.8f))
            }
        }
    }
}

private fun Modifier.pillBackground(c: Color): Modifier =
    this.clip(CircleShape).background(c)