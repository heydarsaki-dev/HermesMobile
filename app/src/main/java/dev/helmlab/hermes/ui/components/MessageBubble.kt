package dev.helmlab.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.helmlab.hermes.data.ChatItem
import dev.helmlab.hermes.data.ToolEventLine

@Composable
fun MessageBubble(
    item: ChatItem,
    showTools: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = item.role == "user"
    val cs = MaterialTheme.colorScheme

    Column(modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isUser) cs.primary else cs.secondary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isUser) "شما" else "H",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                if (isUser) "شما" else "هرمس",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant
            )
        }

        Surface(
            color = if (isUser) cs.primary.copy(alpha = 0.16f) else cs.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(
                    1.dp,
                    (if (item.error) cs.error else cs.outline).copy(alpha = 0.32f),
                    RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 20.dp
                    )
                )
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (item.imageDataUrl != null) {
                    AsyncImage(
                        model = item.imageDataUrl,
                        contentDescription = "پیوست تصویر",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (item.text.isNotBlank() && item.text != "تصویر") Spacer(Modifier.height(8.dp))
                }

                if (item.text.isNotBlank()) {
                    Text(item.text, style = MaterialTheme.typography.bodyMedium)
                }

                if (item.pending && item.text.isBlank()) {
                    Spacer(Modifier.height(6.dp))
                    TypingDots()
                }

                if (item.error) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(13.dp), tint = cs.error)
                        Spacer(Modifier.width(4.dp))
                        Text("خطا در دریافت پاسخ", style = MaterialTheme.typography.labelSmall, color = cs.error)
                    }
                }

                if (showTools && item.toolEvents.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    ToolEventPanel(item.toolEvents)
                }
            }
        }
    }
}

@Composable
private fun ToolEventPanel(events: List<ToolEventLine>) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.background.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Text(
            "ابزارهای اجراشده (${events.size})",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        events.forEach { e ->
            Row(
                Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    null,
                    Modifier.size(12.dp),
                    tint = if (e.error) cs.error else cs.secondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    e.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface
                )
                if (e.seconds > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "%.1fs".format(e.seconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
                if (e.preview.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        e.preview.take(70).replace("\n", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}