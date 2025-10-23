# mDNS Local Network Demo for Android

A simple yet powerful demonstration of local service discovery and communication between Android devices using mDNS (`NsdManager`) and an embedded HTTP server. 📡

This project showcases how to build two Android apps that find and communicate with each other directly over a local Wi-Fi network, without needing a central server or an internet connection.

## Showcase

The project includes two modules: a **Broadcaster** that advertises a service and a **Receiver** that discovers and interacts with it.

| Broadcaster (Server) | Receiver (Client) |
|---|---|
| <video src="https://github.com/user-attachments/assets/b56cc2ac-7aba-4a54-8a22-87a77f3a618f" width="300"/> | <video src="https://github.com/user-attachments/assets/e0741ae0-9b75-4901-9ec6-1eb55ab978ee" width="300"/> |

---

## How It Works

### broadcaster (Server App)  BROADCASTING
* **Local Server**: Starts a lightweight, embedded HTTP server (`NanoHTTPD`) on a random available port.
* **Service Advertisement**: Advertises a service of type `_loop._tcp.` on the local network using Android's `NsdManager`. The service is given a unique name like `Loop-{id}`.
* **API Endpoints**:
    * `GET /info`: Returns the device's ID, name, and version.
    * `GET /ping`: A simple heartbeat endpoint.
    * `POST /command`: Accepts JSON commands (e.g., `{ "type": "volume", "delta": 10 }`).
* **UI**: The user interface, built with Jetpack Compose, shows the service name, port, a live request counter, and controls to start/stop the service.

### receiver (Client App) 📡
* **Service Discovery**: Uses `NsdManager` to discover `_loop._tcp.` services on the same network.
* **Connection**: Resolves the service's host and port to establish communication.
* **Interaction**: Once a device is selected, the app fetches its `/info` and sends a `/ping` request every 5 seconds to maintain a connection.
* **UI**: The UI lists all discovered devices. After selecting a device, it displays its information, the status of the last ping, and a button to send a POST command.

---

## Tech Stack & Libraries
* **Language**: **Kotlin**
* **UI**: **Jetpack Compose**
* **Asynchronous Programming**: **Coroutines**
* **Dependency Injection**: **Hilt**
* **Networking**:
    * **Server**: **NanoHTTPD** for the embedded HTTP server.
    * **Client**: **OkHttp** for making network requests.
    * **Service Discovery**: Native Android **`NsdManager`**.

---

## Getting Started

To run this project, you'll need two Android devices (or one device and an emulator) connected to the **same local network** (e.g., the same Wi-Fi).

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/rrohaill/mdns-android.git](https://github.com/rrohaill/mdns-android.git)
    ```
2.  **Open the project** in the latest stable version of Android Studio.
3.  **Run the `broadcaster` app** on the first device.
4.  **Run the `receiver` app** on the second device.
5.  The **Broadcaster** will start advertising its service, and the **Receiver** will automatically discover and list it. You can then select the service to begin interaction.
