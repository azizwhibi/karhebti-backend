package com.example.karhebti_android.data.websocket

import android.util.Log
import org.json.JSONObject

// Extension function to convert JSONObject to Map
fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        try {
            val value: String = when (val rawValue = this.get(key.toString())) {
                is JSONObject -> rawValue.toString()
                JSONObject.NULL -> ""
                null -> ""
                else -> rawValue.toString()
            }
            map[key.toString()] = value
        } catch (e: Exception) {
            map[key.toString()] = ""
        }
    }
    return map
}

// This is a simplified WebSocket service that can be extended with Socket.io
// The actual Socket.io implementation will work once the dependency is properly synced

class WebSocketService(private val baseUrl: String) {
    private var isConnectedState: Boolean = false
    private var listeners = mutableListOf<NotificationListener>()

    interface NotificationListener {
        fun onNotificationReceived(notification: Map<String, Any>)
        fun onConnectionChanged(isConnected: Boolean)
    }

    fun addListener(listener: NotificationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    fun connect(token: String) {
        if (isConnectedState) {
            Log.d("WebSocket", "Déjà connecté")
            return
        }

        try {
            Log.d("WebSocket", "Tentative de connexion à $baseUrl avec token: ${token.take(20)}...")

            // Implementation placeholder for Socket.io
            // Replace this with actual Socket.io when the library is available
            isConnectedState = true
            onConnect()
        } catch (e: Exception) {
            Log.e("WebSocket", "Erreur de connexion: ${e.message}")
            onConnectError(e)
        }
    }

    fun disconnect() {
        isConnectedState = false
        onDisconnect()
    }

    private fun onConnect() {
        Log.d("WebSocket", "Connecté au serveur WebSocket")
        notifyListeners(isConnected = true)
    }

    private fun onDisconnect() {
        Log.d("WebSocket", "Déconnecté du serveur WebSocket")
        notifyListeners(isConnected = false)
    }

    private fun onConnectError(exception: Exception?) {
        Log.e("WebSocket", "Erreur de connexion: ${exception?.message}")
        notifyListeners(isConnected = false)
    }

    fun onNotificationReceived(data: Any?) {
        try {
            when (data) {
                is JSONObject -> {
                    val notification = data.toMap()
                    Log.d("WebSocket", "Notification reçue: $notification")
                    listeners.forEach { it.onNotificationReceived(notification) }
                }
                is Map<*,*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val notification = data as Map<String, Any>
                    Log.d("WebSocket", "Notification reçue: $notification")
                    listeners.forEach { it.onNotificationReceived(notification) }
                }
                else -> {
                    Log.d("WebSocket", "Données reçues: $data")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Erreur lors du traitement: ${e.message}")
        }
    }

    private fun notifyListeners(isConnected: Boolean) {
        listeners.forEach { it.onConnectionChanged(isConnected) }
    }

    fun isConnected(): Boolean = isConnectedState

    companion object {
        private var instance: WebSocketService? = null

        fun getInstance(baseUrl: String): WebSocketService {
            return instance ?: WebSocketService(baseUrl).also { instance = it }
        }
    }
}
