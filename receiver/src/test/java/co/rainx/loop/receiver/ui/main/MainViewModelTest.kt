package co.rainx.loop.receiver.ui.main

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import co.rainx.loop.receiver.data.ConnectionStatus
import co.rainx.loop.receiver.data.ServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.Inet4Address

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @Mock
    private lateinit var nsdManager: NsdManager

    @Mock
    private lateinit var okHttpClient: OkHttpClient

    @Mock
    private lateinit var call: Call

    @Mock
    private lateinit var response: Response

    @Mock
    private lateinit var serviceInfo: NsdServiceInfo

    @Mock
    private lateinit var host: Inet4Address

        private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler, "io")

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(nsdManager, okHttpClient, testDispatcher, testDispatcher)
        viewModel.skipPortCheck = true // Skip real socket connections in tests
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Mockito.reset(serviceInfo)
    }

    @Test
    fun `test startDiscovery adds service to list`() = testScope.runTest {
        whenever(serviceInfo.serviceName).thenReturn("Test Service")
        whenever(serviceInfo.serviceType).thenReturn("_loop._tcp.")
        whenever(serviceInfo.host).thenReturn(host)
        whenever(host.hostAddress).thenReturn("192.168.1.1")
        val discoveryListenerCaptor =
            argumentCaptor<NsdManager.DiscoveryListener>()

        val channel = Channel<List<ServiceInfo>>(Channel.UNLIMITED)
        val job = launch(testDispatcher) {
            viewModel.services.collect { channel.send(it) }
        }

        viewModel.startDiscovery()
        advanceUntilIdle()

        verify(nsdManager).discoverServices(
            eq("_loop._tcp."),
            eq(NsdManager.PROTOCOL_DNS_SD),
            discoveryListenerCaptor.capture()
        )

        val discoveryListener = discoveryListenerCaptor.firstValue

        val resolveListenerCaptor = argumentCaptor<NsdManager.ResolveListener>()

        discoveryListener.onServiceFound(serviceInfo)
        advanceUntilIdle()

        verify(nsdManager).resolveService(eq(serviceInfo), resolveListenerCaptor.capture())

        val resolveListener = resolveListenerCaptor.firstValue

        resolveListener.onServiceResolved(serviceInfo)
        advanceUntilIdle()

        assertEquals(emptyList<ServiceInfo>(), channel.receive())
        assertEquals(1, channel.receive().size)

        viewModel.cleanup()
        job.cancel()
    }

    @Test
    fun `test stopDiscovery stops service discovery`() = testScope.runTest {
        val discoveryListenerCaptor =
            argumentCaptor<NsdManager.DiscoveryListener>()
        viewModel.startDiscovery()
        advanceUntilIdle()
        verify(nsdManager).discoverServices(any(), any(), discoveryListenerCaptor.capture())
        viewModel.stopDiscovery()
        advanceUntilIdle()
        verify(nsdManager).stopServiceDiscovery(discoveryListenerCaptor.firstValue)

        viewModel.cleanup()
    }

    @Test
    fun `test service lost removes service from list`() = testScope.runTest {
        whenever(serviceInfo.serviceName).thenReturn("Test Service")
        whenever(serviceInfo.serviceType).thenReturn("_loop._tcp.")
        whenever(serviceInfo.host).thenReturn(host)
        whenever(host.hostAddress).thenReturn("192.168.1.1")

        val discoveryListenerCaptor =
            argumentCaptor<NsdManager.DiscoveryListener>()

        val channel = Channel<List<ServiceInfo>>(Channel.UNLIMITED)
        val job = launch(testDispatcher) {
            viewModel.services.collect { channel.send(it) }
        }

        viewModel.startDiscovery()
        advanceUntilIdle()

        verify(nsdManager).discoverServices(
            eq("_loop._tcp."),
            eq(NsdManager.PROTOCOL_DNS_SD),
            discoveryListenerCaptor.capture()
        )

        val discoveryListener = discoveryListenerCaptor.firstValue

        val resolveListenerCaptor = argumentCaptor<NsdManager.ResolveListener>()

        discoveryListener.onServiceFound(serviceInfo)
        advanceUntilIdle()

        verify(nsdManager).resolveService(eq(serviceInfo), resolveListenerCaptor.capture())

        val resolveListener = resolveListenerCaptor.firstValue

        resolveListener.onServiceResolved(serviceInfo)
        advanceUntilIdle()

        assertEquals(emptyList<ServiceInfo>(), channel.receive())
        assertEquals(1, channel.receive().size)

        discoveryListener.onServiceLost(serviceInfo)
        advanceUntilIdle()

        assertEquals(0, channel.receive().size)

        viewModel.cleanup()
        job.cancel()
    }

    @Test
    fun `test selectService updates selected service and connection status`() = testScope.runTest {
        val service = ServiceInfo("Test Service", "192.168.1.1", 8080)

        // Mock HTTP calls to simulate connection failure
        whenever(okHttpClient.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(false)

        val channel = Channel<ServiceInfo?>(Channel.UNLIMITED)
        val job = launch(testDispatcher) {
            viewModel.selectedService.collect { channel.send(it) }
        }

        viewModel.selectService(service)
        advanceUntilIdle()

        assertEquals(null, channel.receive())
        assertEquals(ConnectionStatus.ERROR, channel.receive()?.connectionStatus)

        viewModel.cleanup()
        job.cancel()
    }

    @Test
    fun `test sendCommand sends post request`() = testScope.runTest {
        val service = ServiceInfo(
            "Test Service",
            "192.168.1.1",
            8080,
            connectionStatus = ConnectionStatus.CONNECTED
        )

        whenever(okHttpClient.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(true)

        viewModel.sendCommand(service)
        advanceUntilIdle()

        val requestCaptor = argumentCaptor<Request>()
        verify(okHttpClient).newCall(requestCaptor.capture())

        val request = requestCaptor.firstValue
        assertEquals("POST", request.method)
        assertEquals("http://192.168.1.1:8080/command", request.url.toString())

        viewModel.cleanup()
    }

    @Test
    fun `test selectService with failed connection`() = testScope.runTest {
        val service = ServiceInfo("Test Service", "192.168.1.1", 8080)

        whenever(okHttpClient.newCall(any())).thenReturn(call)
        whenever(call.execute()).thenReturn(response)
        whenever(response.isSuccessful).thenReturn(false)

        val channel = Channel<ServiceInfo?>(Channel.UNLIMITED)
        val job = launch(testDispatcher) {
            viewModel.selectedService.collect { channel.send(it) }
        }

        viewModel.selectService(service)
        advanceUntilIdle()

        assertEquals(null, channel.receive())
        assertEquals(ConnectionStatus.ERROR, channel.receive()?.connectionStatus)

        viewModel.cleanup()
        job.cancel()
    }
}