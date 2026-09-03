package com.gamezop.postmessageexample.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gamezop.postmessageexample.bridge.GameEventBridge

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameWebView(
    url: String,
    onRawEvent: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    reloadSignal: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webView by remember(url) { mutableStateOf<WebView?>(null) }
    var progress by remember(url) { mutableIntStateOf(0) }

    fun openExternal(destination: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, destination.toUri()))
        } catch (_: ActivityNotFoundException) {
            onError("No application can open this link")
        } catch (_: SecurityException) {
            onError("Android blocked this external link")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    webView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)
                        allowContentAccess = false
                        allowFileAccess = false
                    }
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    requestFocusFromTouch()

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(GameEventBridge(onRawEvent), GameEventBridge.NAME)
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            onLoadingChanged(newProgress < 100)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                            onError(null)
                            onLoadingChanged(true)
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            onLoadingChanged(false)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                onLoadingChanged(false)
                                onError(
                                    webViewErrorMessage(
                                        context = viewContext,
                                        errorCode = error?.errorCode,
                                        description = error?.description?.toString(),
                                        host = request.url.host,
                                    ),
                                )
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val destination = request?.url?.toString() ?: return true
                            return if (UrlPolicy.isAllowedNavigation(url, destination)) {
                                false
                            } else {
                                openExternal(destination)
                                true
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { currentWebView ->
                currentWebView.requestFocus()
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (progress in 0..99) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    BackHandler {
        val current = webView
        if (current?.canGoBack() == true) current.goBack() else onClose()
    }

    DisposableEffect(reloadSignal) {
        if (reloadSignal > 0) webView?.reload()
        onDispose { }
    }

    DisposableEffect(lifecycleOwner, url) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView?.apply {
                stopLoading()
                removeJavascriptInterface(GameEventBridge.NAME)
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
            webView = null
        }
    }
}

private fun webViewErrorMessage(
    context: Context,
    errorCode: Int?,
    description: String?,
    host: String?,
): String = when (errorCode) {
    WebViewClient.ERROR_HOST_LOOKUP -> {
        if (!hasInternetConnection(context)) {
            "This device or emulator is offline. Connect it to the internet, then try again."
        } else {
            "Android could not resolve ${host ?: "this hostname"}. Verify the URL, then check the device or emulator DNS settings."
        }
    }

    WebViewClient.ERROR_CONNECT ->
        "Android resolved ${host ?: "the hostname"}, but could not connect to its server."

    WebViewClient.ERROR_TIMEOUT ->
        "The connection to ${host ?: "the server"} timed out."

    WebViewClient.ERROR_FAILED_SSL_HANDSHAKE ->
        "Android rejected the HTTPS certificate for ${host ?: "this page"}."

    else -> description ?: "The page could not be loaded."
}

private fun hasInternetConnection(context: Context): Boolean {
    val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val activeNetwork = connectivity.activeNetwork ?: return false
    val capabilities = connectivity.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
