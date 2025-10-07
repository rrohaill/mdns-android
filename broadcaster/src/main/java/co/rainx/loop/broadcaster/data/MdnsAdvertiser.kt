package co.rainx.loop.broadcaster.data

import android.net.nsd.NsdManager

import android.net.nsd.NsdServiceInfo

class MdnsAdvertiser(private val nsd: NsdManager) {

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceType: String? = null
    private var serviceName: String? = null

    fun register(serviceName: String, serviceType: String, port: Int) {
        this.serviceName = serviceName
        this.serviceType = serviceType

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                this@MdnsAdvertiser.serviceName = serviceInfo.serviceName
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = serviceType
            this.port = port
        }

        nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregister() {
        registrationListener?.let {
            nsd.unregisterService(it)
            registrationListener = null
        }
    }
}
