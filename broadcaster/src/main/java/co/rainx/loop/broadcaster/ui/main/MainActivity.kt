package co.rainx.loop.broadcaster.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                BroadcasterScreen(
                    running = viewModel.running,
                    label = viewModel.label,
                    commandCount = viewModel.commandCount,
                    commands = viewModel.commands,
                    onStart = viewModel::onStart,
                    onStop = viewModel::onStop
                )
            }
        }
    }
}
