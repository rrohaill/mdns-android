package co.rainx.loop.broadcaster.data

data class CommandData(
    val type: String,
    val delta: Int?,
    val timestamp: String,
    val fromIp: String
)
