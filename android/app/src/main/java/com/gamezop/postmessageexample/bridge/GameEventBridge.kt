package com.gamezop.postmessageexample.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import androidx.annotation.Keep

@Keep
class GameEventBridge(
    private val onEvent: (String) -> Unit,
    private val postToMain: ((() -> Unit) -> Unit) = { action ->
        Handler(Looper.getMainLooper()).post(action)
    },
) {
    @Keep
    @JavascriptInterface
    fun postMessage(gameEvent: String) {
        postToMain { onEvent(gameEvent) }
    }

    companion object {
        const val NAME = "AndroidBridge"
    }
}
