package com.gamezop.postmessageexample

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamezop.postmessageexample.bridge.GameEventBridge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PostMessageFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun invalidUrlIsExplained() {
        composeRule.onNodeWithTag("url-input").performTextInput("http://insecure.example")
        composeRule.onNodeWithTag("launch-game").performClick()

        composeRule.onNodeWithTag("url-input").assertTextContains("Only HTTPS URLs are accepted")
    }

    @Test
    fun individualAndBattlesTabsToggleTheirLaunchBlocks() {
        composeRule.onNodeWithText("Individual game URL").assertIsDisplayed()
        composeRule.onNodeWithTag("mode-battles").performClick()
        composeRule.onNodeWithText("Final Battles URL").assertIsDisplayed()
        composeRule.onNodeWithTag("mode-individual").performClick()
        composeRule.onNodeWithText("Individual game URL").assertIsDisplayed()
    }

    @Test
    fun battlesRequiresDecodableRoomDetails() {
        composeRule.onNodeWithTag("mode-battles").performClick()
        composeRule.onNodeWithTag("url-input").performTextInput("https://1234.play.gamezop.com/game")
        composeRule.onNodeWithTag("launch-game").performClick()

        composeRule.onNodeWithTag("url-input").assertTextContains("Battles URL must include roomDetails")
    }

    @Test
    fun launchedGameHasAnExplicitEventLogToggle() {
        composeRule.onNodeWithTag("url-input").performTextInput("https://1234.play.gamezop.com/game")
        composeRule.onNodeWithTag("launch-game").performClick()

        composeRule.onNodeWithTag("event-log-toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("event-log-toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("event-log-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("event-log-toggle").performClick()
        composeRule.onNodeWithTag("event-log-panel").assertDoesNotExist()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Test
    fun javascriptCallsRenamedBridgeInARealWebView() {
        val pageReady = CountDownLatch(1)
        val callbackReady = CountDownLatch(1)
        val received = AtomicReference<String>()
        lateinit var webView: WebView

        composeRule.runOnUiThread {
            webView = WebView(composeRule.activity).apply {
                settings.javaScriptEnabled = true
                addJavascriptInterface(
                    GameEventBridge(onEvent = { payload ->
                        received.set(payload)
                        callbackReady.countDown()
                    }),
                    GameEventBridge.NAME,
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady.countDown()
                    }
                }
                loadDataWithBaseURL(
                    "https://bridge.test/",
                    "<html><body>Bridge test</body></html>",
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
            composeRule.activity.setContentView(webView)
        }
        assertTrue("Test page did not load", pageReady.await(5, TimeUnit.SECONDS))

        composeRule.runOnUiThread {
            webView.evaluateJavascript(
                "AndroidBridge.postMessage(JSON.stringify({state:'loaded',gameCode:'instrumented'}))",
                null,
            )
        }
        assertTrue("Bridge callback was not received", callbackReady.await(5, TimeUnit.SECONDS))
        assertEquals("loaded", JSONObject(received.get()).getString("state"))

        composeRule.runOnUiThread {
            webView.removeJavascriptInterface(GameEventBridge.NAME)
            webView.destroy()
        }
    }
}
