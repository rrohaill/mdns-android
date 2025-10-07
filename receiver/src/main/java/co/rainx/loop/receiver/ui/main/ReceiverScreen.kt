package co.rainx.loop.receiver.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.rainx.loop.receiver.data.ServiceInfo

@Composable
fun ReceiverScreen(
    services: List<ServiceInfo>,
    selectedService: ServiceInfo?,
    onServiceSelected: (ServiceInfo) -> Unit,
    onSendCommand: (ServiceInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "loop receiver",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Discovered Services:")
        LazyColumn {
            items(services) { service ->
                Button(onClick = { onServiceSelected(service) }) {
                    Text("${service.name} at ${service.host}:${service.port}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedService?.let { service ->
            Text("Selected Service:")
            Text("Name: ${service.name}")
            Text("Host: ${service.host}")
            Text("Port: ${service.port}")
            Text("ID: ${service.id}")
            Text("Version: ${service.version}")
            Text("Last Ping: ${service.lastPing}")
            Button(onClick = { onSendCommand(service) }) {
                Text("Send Command")
            }
        }
    }
}