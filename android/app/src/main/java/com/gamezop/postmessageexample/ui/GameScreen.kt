package com.gamezop.postmessageexample.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.gamezop.postmessageexample.model.CapturedGameEvent
import com.gamezop.postmessageexample.model.EventFamily
import com.gamezop.postmessageexample.model.EventLogState
import com.gamezop.postmessageexample.model.GameMode
import com.gamezop.postmessageexample.model.ValidationStatus
import com.gamezop.postmessageexample.web.GameWebView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    url: String,
    mode: GameMode,
    eventState: EventLogState,
    onRawEvent: (String) -> Unit,
    onSelectEvent: (Long) -> Unit,
    onClearEvents: () -> Unit,
    onClose: () -> Unit,
) {
    var isLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var reloadSignal by remember(url) { mutableIntStateOf(0) }
    var showLogs by rememberSaveable(url) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Surface(shadowElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close-game"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close game",
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${mode.label} game",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isLoading) "Loading…" else "AndroidBridge ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { reloadSignal++ },
                    modifier = Modifier.testTag("reload-game"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reload game",
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            GameWebView(
                url = url,
                onRawEvent = onRawEvent,
                onLoadingChanged = { isLoading = it },
                onError = { error = it },
                reloadSignal = reloadSignal,
                onClose = onClose,
            )

            error?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Could not load page", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            error = null
                            reloadSignal++
                        }) {
                            Text("Try again")
                        }
                    }
                }
            }

            if (showLogs) {
                val panelHeight = (maxHeight * 0.44f).coerceIn(160.dp, 300.dp)
                EventInspector(
                    state = eventState,
                    onSelectEvent = onSelectEvent,
                    onClearEvents = onClearEvents,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(panelHeight),
                )
            }

            val density = LocalDensity.current
            FloatingLogPill(
                eventCount = eventState.events.size,
                logsVisible = showLogs,
                containerWidthPx = with(density) { maxWidth.toPx() },
                containerHeightPx = with(density) { maxHeight.toPx() },
                onClick = { showLogs = !showLogs },
            )
        }
    }
}

@Composable
private fun EventInspector(
    state: EventLogState,
    onSelectEvent: (Long) -> Unit,
    onClearEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.events.firstOrNull { it.id == state.selectedEventId }
        ?: state.events.lastOrNull()

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("event-log-panel"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (state.events.isEmpty()) MaterialTheme.colorScheme.outline
                        else Color(0xFF1B9C68),
                        CircleShape,
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Event logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.events.size} captured ${if (state.events.size == 1) "event" else "events"}",
                    modifier = Modifier.testTag("event-count"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onClearEvents,
                enabled = state.events.isNotEmpty(),
                modifier = Modifier.testTag("clear-events"),
            ) {
                Text("Clear")
            }
        }

        if (state.droppedEventCount > 0) {
            Text(
                text = "${state.droppedEventCount} older events discarded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Waiting for Gamezop events", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The bridge is registered as AndroidBridge.postMessage.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 530.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("event-timeline"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events.asReversed(), key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            selected = selected?.id == event.id,
                            onClick = { onSelectEvent(event.id) },
                        )
                    }
                }
                selected?.let {
                    EventDetails(
                        event = it,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Draggable, safe-area-contained entry point for the event overlay. */
@Composable
private fun FloatingLogPill(
    eventCount: Int,
    logsVisible: Boolean,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val pillWidthPx = with(density) { 92.dp.toPx() }
    val pillHeightPx = with(density) { 48.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }
    val maxX = (containerWidthPx - pillWidthPx).coerceAtLeast(0f)
    val maxY = (containerHeightPx - pillHeightPx).coerceAtLeast(0f)
    var offsetX by remember { mutableFloatStateOf(Float.NaN) }
    var offsetY by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(maxX, maxY) {
        offsetX = if (offsetX.isNaN()) maxX - marginPx.coerceAtMost(maxX) else offsetX.coerceIn(0f, maxX)
        offsetY = if (offsetY.isNaN()) marginPx.coerceAtMost(maxY) else offsetY.coerceIn(0f, maxY)
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = offsetX.takeUnless { it.isNaN() }?.roundToInt() ?: 0,
                    y = offsetY.takeUnless { it.isNaN() }?.roundToInt() ?: 0,
                )
            }
            .size(width = 92.dp, height = 48.dp)
            .pointerInput(maxX, maxY) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxY)
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("event-log-toggle"),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE6111A2E),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0x5567E8F9)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (logsVisible) "Hide" else "Logs",
                color = Color(0xFF67E8F9),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = eventCount.toString(),
                modifier = Modifier
                    .background(Color(0xFF263451), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                color = Color(0xFFBFDBFE),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EventCard(
    event: CapturedGameEvent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = eventColor(event)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("event-${event.id}"),
        border = if (selected) BorderStroke(2.dp, accent) else null,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                event.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                "${event.family.name.lowercase()} · ${formatTime(event.receivedAtEpochMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventDetails(event: CapturedGameEvent, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("event-details"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    event.status.name.lowercase(),
                    color = eventColor(event),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(event.rawJson)) },
                modifier = Modifier.testTag("copy-event"),
            ) {
                Text("Copy JSON")
            }
        }
        Spacer(Modifier.height(12.dp))
        event.payload.displayFields.forEach { field ->
            Text(field.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            SelectionContainer {
                Text(field.value, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text("Raw JSON", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
        ) {
            SelectionContainer {
                Text(
                    text = event.prettyJson,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun eventColor(event: CapturedGameEvent): Color = when {
    event.status == ValidationStatus.MALFORMED -> MaterialTheme.colorScheme.error
    event.status == ValidationStatus.UNKNOWN -> Color(0xFFD17A00)
    event.family == EventFamily.BATTLES -> Color(0xFF6750A4)
    event.family == EventFamily.INDIVIDUAL -> Color(0xFF00796B)
    else -> MaterialTheme.colorScheme.outline
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(epochMillis))
