package com.gamezop.postmessageexample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamezop.postmessageexample.data.GameEventViewModel
import com.gamezop.postmessageexample.model.GameMode

@Composable
fun GamezopPostMessageApp(viewModel: GameEventViewModel) {
    val eventState by viewModel.state.collectAsStateWithLifecycle()
    var activeUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var activeModeName by rememberSaveable { mutableStateOf(GameMode.INDIVIDUAL.name) }

    val activeMode = remember(activeModeName) {
        GameMode.entries.firstOrNull { it.name == activeModeName } ?: GameMode.INDIVIDUAL
    }

    if (activeUrl == null) {
        LauncherScreen(
            onLaunch = { mode, url ->
                activeModeName = mode.name
                activeUrl = url.trim()
            },
        )
    } else {
        GameScreen(
            url = requireNotNull(activeUrl),
            mode = activeMode,
            eventState = eventState,
            onRawEvent = viewModel::capture,
            onSelectEvent = viewModel::select,
            onClearEvents = viewModel::clear,
            onClose = { activeUrl = null },
        )
    }
}
