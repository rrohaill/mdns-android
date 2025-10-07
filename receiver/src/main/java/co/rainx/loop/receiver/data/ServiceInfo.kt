package co.rainx.loop.receiver.data

data class ServiceInfo(
    val name: String,
    val host: String,
    val port: Int,
    val id: String? = null,
    val version: Int? = null,
    val lastPing: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN
)

enum class ConnectionStatus {
    UNKNOWN,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}
