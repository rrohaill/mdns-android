--------------------
# Done -> End-to-End Local Server + mDNS (Android → Android)

The project has been successfully implemented as per the requirements. Both the broadcaster and receiver applications are fully functional and ready for use. Hilt has been added for dependency injection. To run the applications, open the project in Android Studio and run the broadcaster and receiver modules on two separate devices connected to the same network. The broadcaster will start advertising the service, and the receiver will discover and display it, allowing for seamless interaction between the two.

| Broadcaster | Receiver |
|---|---|
| <video src="https://github.com/user-attachments/assets/b56cc2ac-7aba-4a54-8a22-87a77f3a618f" width="300"/> | <video src="https://github.com/user-attachments/assets/e0741ae0-9b75-4901-9ec6-1eb55ab978ee" width="300"/> |


# Take-home — End-to-End Local Server + mDNS (Android → Android)

**Timebox:** 1–2 days (weekend friendly)  
**Goal:** Build two Android apps that communicate locally over Wi‑Fi/hotspot via mDNS (NsdManager) + HTTP.

## Apps to build
1) **Broadcaster / Server**
   - Starts a local HTTP server on a random port.
   - Advertises `_loop._tcp.` via `NsdManager` with service name `Loop-{id}`.
   - Endpoints:
     - `GET /info` → `{ id, name, version }`
     - `GET /ping` → `{ ok: true }`
     - `POST /command` with JSON `{ type: "volume", delta: Int }` → `{ status: "ok" }`
   - UI shows: service name + port, request counter, start/stop controls.

2) **Receiver / Client**
   - Discovers `_loop._tcp.` services with `NsdManager`, resolves host/port (prefer IPv4).
   - On selection: fetch `/info`, then heartbeat `/ping` every 5s.
   - UI shows: list of devices, selected device info, last ping, button to POST `/command`.

> Must run locally on two devices (or device + emulator on same LAN). No cloud.

## Requirements
- Kotlin + Coroutines, Compose UI.
- Broadcaster uses embedded HTTP (NanoHTTPD). Receiver uses OkHttp.
- Clean lifecycle: stop discovery when leaving, unregister service on stop.
- Clean re-registration and discovery restarts.

## Deliverables
- Working broadcaster and receiver apps.
- Any additions are welcome.
