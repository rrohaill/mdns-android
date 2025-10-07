package co.rainx.loop.broadcaster.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.rainx.loop.broadcaster.data.CommandData

@Composable
fun BroadcasterScreen(
    running: Boolean,
    label: String,
    commandCount: Int,
    commands: List<CommandData>,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "loop broadcaster",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text("Advertises _loop._tcp. and serves /info, /ping, /command over HTTP")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStart,
                enabled = !running
            ) {
                Text("Start")
            }

            OutlinedButton(
                onClick = onStop,
                enabled = running
            ) {
                Text("Stop")
            }
        }

        HorizontalDivider()

        Text("Service: $label")

        Text("Commands handled: $commandCount")

        LazyColumn {
            items(commands) { command ->
                Text("type: ${command.type}, delta: ${command.delta}, from: ${command.fromIp} at ${command.timestamp}")
            }
        }
    }
}

@Preview
@Composable
fun BroadcasterScreenPreview() {
    MaterialTheme {
        BroadcasterScreen(
            running = false,
            label = "—",
            commandCount = 0,
            commands = emptyList(),
            onStart = {},
            onStop = {}
        )
    }
}