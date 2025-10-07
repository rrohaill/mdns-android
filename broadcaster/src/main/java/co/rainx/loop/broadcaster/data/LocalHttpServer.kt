package co.rainx.loop.broadcaster.data

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import java.net.ServerSocket
import java.net.Inet4Address

class LocalHttpServer(private var portHint: Int, private val id: String) : NanoHTTPD(portHint) {

    companion object {
        private fun getWiFiIpAddress(): String? {
            return try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.name.contains("wlan") && it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                    ?.hostAddress
            } catch (e: Exception) {
                Timber.w(e, "Could not get WiFi IP address")
                null
            }
        }

        @Volatile
        var commandsHandled: Int = 0
            private set

        val commands = mutableListOf<CommandData>()
    }

    private val name = "Loop-$id"
    private val version = 1

    fun startAndGetPort(): Int {
        // Pick a random port if 0 and start server
        if (portHint == 0) {
            val socket = ServerSocket(0)
            portHint = socket.localPort
            socket.close()
        }

        // Test if we can create a server socket on this port
        try {
            val testSocket = ServerSocket(portHint, 50, InetAddress.getByName("0.0.0.0"))
            testSocket.close()
            Timber.d("✅ Successfully tested server socket creation on port $portHint")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to create test server socket on port $portHint")
        }

        Timber.d("Starting HTTP server on all interfaces (0.0.0.0):$portHint")
        Timber.d("Server will be accessible at WiFi IP: ${getWiFiIpAddress()}:$portHint")

        // Log network interfaces for debugging
        try {
            NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
                networkInterface.inetAddresses.toList().forEach { address ->
                    if (!address.isLoopbackAddress && !address.isLinkLocalAddress) {
                        Timber.d("Available network interface: ${address.hostAddress} on ${networkInterface.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not enumerate network interfaces")
        }

        try {
            Timber.d("Attempting to start NanoHTTPD server...")
            start(SOCKET_READ_TIMEOUT, false)
            
            // Wait a moment for server to fully start
            Thread.sleep(100)
            
            // Update portHint to the actual listening port
            val actualPort = listeningPort
            if (actualPort != portHint) {
                Timber.w("⚠️ Port mismatch! Expected: $portHint, Actual: $actualPort")
                portHint = actualPort
            }
            
            Timber.d("HTTP server started successfully on port $actualPort")
            Timber.d("Server alive status: ${isAlive}")
            Timber.d("Server listening port: ${listeningPort}")
            Timber.d("Server hostname: ${hostname ?: "not set"}")
            
            // Verify the server is actually listening
            if (isAlive) {
                Timber.d("✅ Server is alive and listening")
            } else {
                Timber.w("⚠️ Server started but reports not alive")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to start HTTP server on port $portHint")
            throw e
        }

        // Test local connectivity after a brief delay on background thread  
        Thread {
            Thread.sleep(500) // Give server time to fully initialize
            testLocalConnectivity()
        }.start()
        
        return portHint
    }

    private fun testLocalConnectivity() {
        // Test localhost first
        testConnection("127.0.0.1", "localhost")

        // Test the actual WiFi IP
        getWiFiIpAddress()?.let { wifiIp ->
            testConnection(wifiIp, "WiFi IP")
        }
    }

    private fun testConnection(host: String, description: String) {
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, portHint), 2000)
            socket.close()
            Timber.d("✅ $description connectivity test passed ($host:$portHint)")
        } catch (e: java.net.ConnectException) {
            Timber.w("❌ $description connectivity test failed: Connection refused to $host:$portHint")
        } catch (e: java.net.SocketTimeoutException) {
            Timber.w("❌ $description connectivity test failed: Timeout to $host:$portHint")
        } catch (e: Exception) {
            Timber.w("❌ $description connectivity test failed: ${e.javaClass.simpleName} to $host:$portHint - ${e.message ?: "Unknown error"}")
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri
        val method = session.method
        val remoteIp = session.remoteIpAddress
        val headers = session.headers

        Timber.d("🌐 HTTP Request: $method $path from $remoteIp")
        Timber.d("Headers: $headers")

        return when {
            method == Method.GET && path == "/info" -> {
                ok(mapOf("id" to id, "name" to name, "version" to version))
            }

            method == Method.GET && path == "/ping" -> {
                ok(mapOf("ok" to true))
            }

            method == Method.POST && path == "/command" -> {
                Timber.d("💬 Processing /command request...")
                try {
                    val body = mutableMapOf<String, String>()
                    session.parseBody(body)
                    
                    Timber.d("Parsed body keys: ${body.keys}")
                    body.forEach { (key, value) ->
                        Timber.d("Body[$key] = $value")
                    }
                    
                    val postData = body["postData"]
                    if (postData != null) {
                        Timber.d("Post data: $postData")
                        val json = JSONObject(postData)
                        val type = json.getString("type")
                        val delta = json.optInt("delta")
                        Timber.d("Command type: $type")

                        if (type == "volume") {
                            commandsHandled++
                            val command = CommandData(
                                type = type,
                                delta = delta,
                                timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                fromIp = remoteIp
                            )
                            commands.add(command)
                            Timber.d("✅ Volume command handled! Total commands: $commandsHandled")
                            ok(mapOf("status" to "ok"))
                        } else {
                            Timber.w("Unknown command type: $type")
                            newFixedLengthResponse(
                                Response.Status.BAD_REQUEST,
                                "application/json",
                                "bad request"
                            )
                        }
                    } else {
                        Timber.w("No postData found in body")
                        newFixedLengthResponse(
                            Response.Status.BAD_REQUEST,
                            "application/json",
                            "no post data"
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing /command request")
                    newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "bad request: ${e.message}"
                    )
                }
            }

            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "application/json",
                "not found"
            )
        }
    }

    private fun ok(json: Map<String, Any>): Response {
        val body = JSONObject(json).toString()
        return newFixedLengthResponse(Response.Status.OK, "application/json", body)
    }
}
