package com.gamezop.postmessageexample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamezop.postmessageexample.model.GameMode
import com.gamezop.postmessageexample.web.UrlPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    onLaunch: (GameMode, String) -> Unit,
) {
    var modeName by rememberSaveable { mutableStateOf(GameMode.INDIVIDUAL.name) }
    var individualUrl by rememberSaveable { mutableStateOf("") }
    var battlesUrl by rememberSaveable { mutableStateOf("") }
    var attemptedLaunch by rememberSaveable { mutableStateOf(false) }
    val mode = GameMode.entries.firstOrNull { it.name == modeName } ?: GameMode.INDIVIDUAL
    val url = if (mode == GameMode.INDIVIDUAL) individualUrl else battlesUrl
    val validationError = UrlPolicy.validationError(url, mode)

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Gamezop Event Bridge",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose Individual or Battles, launch a real Gamezop URL, and inspect bridge events in real time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))

                ScrollableTabRow(
                    selectedTabIndex = mode.ordinal,
                    edgePadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mode-tabs"),
                ) {
                    GameMode.entries.forEach { candidate ->
                        Tab(
                            selected = mode == candidate,
                            onClick = {
                                modeName = candidate.name
                                attemptedLaunch = false
                            },
                            text = { Text(candidate.label) },
                            modifier = Modifier.testTag("mode-${candidate.name.lowercase()}"),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = if (mode == GameMode.INDIVIDUAL) "Individual game URL" else "Final Battles URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (mode == GameMode.INDIVIDUAL) {
                                "Paste a Gamezop game URL or Unique Link."
                            } else {
                                "Paste the generated Battles URL containing roomDetails."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                if (mode == GameMode.INDIVIDUAL) individualUrl = it else battlesUrl = it
                                attemptedLaunch = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url-input"),
                            singleLine = true,
                            label = { Text("https://…") },
                            isError = attemptedLaunch && validationError != null,
                            supportingText = {
                                if (attemptedLaunch && validationError != null) Text(validationError)
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                attemptedLaunch = true
                                if (validationError == null) onLaunch(mode, url)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("launch-game"),
                        ) {
                            Text("Launch ${mode.label}")
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Events stay on this device only and are cleared when the app process ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
