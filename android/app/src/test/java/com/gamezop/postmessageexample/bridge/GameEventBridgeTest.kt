package com.gamezop.postmessageexample.bridge

import android.webkit.JavascriptInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GameEventBridgeTest {
    @Test
    fun `bridge contract names are exact`() {
        assertEquals("AndroidBridge", GameEventBridge.NAME)

        val method = GameEventBridge::class.java.getDeclaredMethod("postMessage", String::class.java)
        assertNotNull(method.getAnnotation(JavascriptInterface::class.java))
    }

    @Test
    fun `bridge forwards the unmodified payload through dispatcher`() {
        var received: String? = null
        var dispatched = false
        val bridge = GameEventBridge(
            onEvent = { received = it },
            postToMain = { action ->
                dispatched = true
                action()
            },
        )

        bridge.postMessage("""{"state":"loaded"}""")

        assertEquals(true, dispatched)
        assertEquals("""{"state":"loaded"}""", received)
    }
}
