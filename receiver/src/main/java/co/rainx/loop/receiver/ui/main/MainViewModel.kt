package co.rainx.loop.receiver.ui.main

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.rainx.loop.receiver.data.ConnectionStatus
import co.rainx.loop.receiver.data.ServiceInfo
import co.rainx.loop.receiver.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val nsd: NsdManager,
    private val http: OkHttpClient,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _services = MutableStateFlow<List<ServiceInfo>>(emptyList())
    val services = _services.asStateFlow()

    private val _selectedService = MutableStateFlow<ServiceInfo?>(null)
    val selectedService = _selectedService.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // For testing - allows bypassing port check
    var skipPortCheck = false

    fun startDiscovery() {
        Timber.d("Starting mDNS service discovery for _loop._tcp.")
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("Discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("Service found: ${service.serviceName}")
                nsd.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(
                        serviceInfo: NsdServiceInfo,
                        errorCode: Int
                    ) {
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host
                        if (host is Inet4Address) {
                            val newService = ServiceInfo(
                                name = serviceInfo.serviceName,
                                host = host.hostAddress.orEmpty(),
                                port = serviceInfo.port
                            )
                            Timber.d("Discovered service: ${newService.name} at ${newService.host}:${newService.port}")
                            _services.value = (_services.value + newService).distinctBy { it.name }
                        } else {
                            Timber.w("Service ${serviceInfo.serviceName} has non-IPv4 host: $host")
                        }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _services.value = _services.value.filter { it.name != service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        nsd.discoverServices("_loop._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let { nsd.stopServiceDiscovery(it) }
    }

    fun selectService(service: ServiceInfo) {
        viewModelScope.launch(dispatcher) {
            // Update service status to connecting
            val connectingService = service.copy(connectionStatus = ConnectionStatus.CONNECTING)
            updateServiceInList(connectingService)
            _selectedService.value = connectingService // Set selectedService to connecting state

            // First check if the port is actually open (unless skipped for testing)
            if (!skipPortCheck) {
                Timber.d("Checking if ${service.host}:${service.port} is reachable...")
                if (!isPortOpen(service.host, service.port)) {
                    handleServiceError(
                        service,
                        "Port ${service.port} is not open or service is not running"
                    )
                    return@launch
                }
            }

            val request =
                Request.Builder().url("http://${service.host}:${service.port}/info").build()
            try {
                val response = http.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val id = json.getString("id")
                        val version = json.getInt("version")
                        val updatedService = service.copy(
                            id = id,
                            version = version,
                            connectionStatus = ConnectionStatus.CONNECTED
                        )
                        _selectedService.value = updatedService
                        updateServiceInList(updatedService)
                        startHeartbeat(updatedService)
                    } else {
                        _selectedService.value = null
                        handleServiceError(service, "Empty response body")
                    }
                } else {
                    _selectedService.value = null
                    handleServiceError(service, "HTTP ${response.code}: ${response.message}")
                }
            } catch (e: ConnectException) {
                handleServiceError(service, "Connection refused: Service not reachable")
                Timber.e(e, "Connection refused to ${service.host}:${service.port}")
            } catch (e: SocketTimeoutException) {
                handleServiceError(service, "Connection timeout: Service not responding")
                Timber.e(e, "Timeout connecting to ${service.host}:${service.port}")
            } catch (e: UnknownHostException) {
                handleServiceError(service, "Host not found: ${service.host}")
                Timber.e(e, "Unknown host: ${service.host}")
            } catch (e: Exception) {
                handleServiceError(service, "Network error: ${e.message}")
                Timber.e(e, "Unexpected error connecting to ${service.host}:${service.port}")
            }
        }
    }

    private fun handleServiceError(service: ServiceInfo, errorMessage: String) {
        val errorService = service.copy(connectionStatus = ConnectionStatus.ERROR)
        updateServiceInList(errorService)
        _selectedService.value = errorService
        Timber.w("Service error for ${service.name}: $errorMessage")
    }

    private fun updateServiceInList(updatedService: ServiceInfo) {
        _services.value = _services.value.map {
            if (it.name == updatedService.name) updatedService else it
        }
    }

    private fun startHeartbeat(service: ServiceInfo) {
        viewModelScope.launch(ioDispatcher) {
            var consecutiveFailures = 0
            val maxFailures = 3

            while (_selectedService.value?.name == service.name) {
                val request =
                    Request.Builder().url("http://${service.host}:${service.port}/ping").build()
                try {
                    val response = http.newCall(request).execute()
                    if (response.isSuccessful) {
                        consecutiveFailures = 0
                        val lastPing = LocalDateTime.now()
                        val formattedDate =
                            lastPing.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                        val updatedService = _selectedService.value?.copy(
                            lastPing = formattedDate,
                            connectionStatus = ConnectionStatus.CONNECTED
                        )
                        _selectedService.value = updatedService
                        updatedService?.let { updateServiceInList(it) }
                    } else {
                        consecutiveFailures++
                        Timber.w("Heartbeat failed: HTTP ${response.code} for ${service.name}")
                        if (consecutiveFailures >= maxFailures) {
                            handleHeartbeatFailure(service, "Too many heartbeat failures")
                            break
                        }
                    }
                } catch (e: ConnectException) {
                    consecutiveFailures++
                    Timber.w(e, "Heartbeat connection refused for ${service.name}")
                    if (consecutiveFailures >= maxFailures) {
                        handleHeartbeatFailure(service, "Service unreachable")
                        break
                    }
                } catch (e: SocketTimeoutException) {
                    consecutiveFailures++
                    Timber.w(e, "Heartbeat timeout for ${service.name}")
                    if (consecutiveFailures >= maxFailures) {
                        handleHeartbeatFailure(service, "Service not responding")
                        break
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    Timber.e(e, "Heartbeat error for ${service.name}")
                    if (consecutiveFailures >= maxFailures) {
                        handleHeartbeatFailure(service, "Network error: ${e.message}")
                        break
                    }
                }
                delay(5000)
            }
        }
    }

    private fun handleHeartbeatFailure(service: ServiceInfo, reason: String) {
        val disconnectedService = service.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
        updateServiceInList(disconnectedService)

        if (_selectedService.value?.name == service.name) {
            _selectedService.value = disconnectedService
        }

        Timber.w("Heartbeat stopped for ${service.name}: $reason")
    }

    fun sendCommand(service: ServiceInfo) {
        viewModelScope.launch(dispatcher) {
            // Only send command if service is connected
            if (service.connectionStatus != ConnectionStatus.CONNECTED) {
                Timber.w("Cannot send command to ${service.name}: Service not connected")
                return@launch
            }

            val json = JSONObject().apply {
                put("type", "volume")
                put("delta", 1)
            }
            val jsonString = json.toString()
            Timber.d("💬 Sending command to ${service.name}: $jsonString")

            val body = jsonString.toRequestBody("application/json".toMediaType())
            val request =
                Request.Builder().url("http://${service.host}:${service.port}/command")
                    .post(body)
                    .build()

            Timber.d("Request URL: ${request.url}")
            Timber.d("Request method: ${request.method}")
            try {
                val response = http.newCall(request).execute()
                if (response.isSuccessful) {
                    Timber.d("Command sent successfully to ${service.name}")
                } else {
                    Timber.w("Command failed: HTTP ${response.code} for ${service.name}")
                }
            } catch (e: ConnectException) {
                Timber.e(e, "Command failed: Connection refused to ${service.name}")
                handleServiceError(service, "Connection lost during command")
            } catch (e: SocketTimeoutException) {
                Timber.e(e, "Command failed: Timeout sending to ${service.name}")
            } catch (e: Exception) {
                Timber.e(e, "Command failed: Network error for ${service.name}")
            }
        }
    }

    private suspend fun isPortOpen(host: String, port: Int, timeoutMs: Int = 3000): Boolean {
        return withContext(ioDispatcher) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                Timber.d("Port check failed for $host:$port - ${e.message}")
                false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }

    fun cleanup() {
        onCleared()
    }

}
