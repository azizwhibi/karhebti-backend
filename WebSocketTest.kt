import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

// Test 1: Simple WebSocket client test
fun testRawWebSocket() {
    println("Testing raw WebSocket connection to ws://192.168.1.190:3000")

    val client = object : WebSocketClient(URI("ws://192.168.1.190:3000")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            println("✅ Connected to WebSocket!")
            println("HTTP Status: ${handshakedata?.httpStatus}")
            println("HTTP Message: ${handshakedata?.httpStatusMessage}")
        }

        override fun onMessage(message: String?) {
            println("📨 Message received: $message")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("❌ Connection closed: $code - $reason")
        }

        override fun onError(ex: Exception?) {
            println("❌ Error: ${ex?.message}")
            ex?.printStackTrace()
        }
    }

    client.connect()
}

// Test 2: Try with /chat path
fun testWebSocketWithPath() {
    println("Testing WebSocket connection to ws://192.168.1.190:3000/chat")

    val client = object : WebSocketClient(URI("ws://192.168.1.190:3000/chat")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            println("✅ Connected to WebSocket!")
        }

        override fun onMessage(message: String?) {
            println("📨 Message received: $message")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("❌ Connection closed: $code - $reason")
        }

        override fun onError(ex: Exception?) {
            println("❌ Error: ${ex?.message}")
        }
    }

    client.connect()
}

// Test 3: Try Socket.IO
fun testSocketIO() {
    println("Testing Socket.IO connection to http://192.168.1.190:3000")

    try {
        val socket = IO.socket("http://192.168.1.190:3000")

        socket.on(Socket.EVENT_CONNECT) {
            println("✅ Socket.IO Connected!")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            println("❌ Socket.IO Error: ${args[0]}")
        }

        socket.connect()
    } catch (e: Exception) {
        println("❌ Exception: ${e.message}")
    }
}

fun main() {
    println("=== WebSocket Connection Tests ===\n")

    println("Test 1: Raw WebSocket (root)")
    testRawWebSocket()
    Thread.sleep(3000)

    println("\nTest 2: Raw WebSocket (/chat)")
    testWebSocketWithPath()
    Thread.sleep(3000)

    println("\nTest 3: Socket.IO")
    testSocketIO()
    Thread.sleep(3000)
}

