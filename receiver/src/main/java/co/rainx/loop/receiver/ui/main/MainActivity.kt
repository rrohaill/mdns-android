package co.rainx.loop.receiver.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val services by viewModel.services.collectAsState()
                val selectedService by viewModel.selectedService.collectAsState()
                ReceiverScreen(
                    services = services,
                    selectedService = selectedService,
                    onServiceSelected = viewModel::selectService,
                    onSendCommand = viewModel::sendCommand
                )
            }
        }
        viewModel.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDiscovery()
    }
}
