package co.rainx.loop.broadcaster.ui.main

import android.net.nsd.NsdManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.rainx.loop.broadcaster.data.CommandData
import co.rainx.loop.broadcaster.data.LocalHttpServer
import co.rainx.loop.broadcaster.data.MdnsAdvertiser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val nsd: NsdManager
) : ViewModel() {

    var running by mutableStateOf(false)
    var label by mutableStateOf("—")
    var commandCount by mutableStateOf(0)
    var commands by mutableStateOf(emptyList<CommandData>())

    private var advertiser: MdnsAdvertiser? = null
    private var server: LocalHttpServer? = null
    private val id = UUID.randomUUID().toString().substring(0, 4)

    fun onStart() {
        val localServer = LocalHttpServer(0, id)
        val actualPort = localServer.startAndGetPort()
        server = localServer

        Thread.sleep(1000)

        val serviceName = "Loop-$id"
        val serviceType = "_loop._tcp."
        val mndsAdvertiser = MdnsAdvertiser(nsd)
        mndsAdvertiser.register(serviceName, serviceType, actualPort)
        advertiser = mndsAdvertiser

        label = "$serviceName:$actualPort"
        running = true
        startCommandUpdates()
    }

    fun onStop() {
        advertiser?.unregister()
        advertiser = null
        server?.stop()
        server = null
        running = false
        label = "—"
        commandCount = 0
        commands = emptyList()
    }

    private fun startCommandUpdates() {
        viewModelScope.launch {
            while (running) {
                commandCount = LocalHttpServer.commandsHandled
                commands = LocalHttpServer.commands
                delay(1000) // Update every second
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        onStop()
    }
}
