import SwiftUI
import WebKit

@MainActor
final class WebViewSession: ObservableObject {
    weak var webView: WKWebView?
    @Published var canGoBack = false

    func reload() { webView?.reload() }
    func goBack() { webView?.goBack() }
}

struct GameWebView: UIViewRepresentable {
    let url: URL
    let session: WebViewSession
    let onEvent: (Any) -> Void
    let onLoadingChanged: (Bool) -> Void
    let onError: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            launchURL: url,
            session: session,
            onEvent: onEvent,
            onLoadingChanged: onLoadingChanged,
            onError: onError
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.websiteDataStore = .default()
        configuration.userContentController.add(context.coordinator, name: Coordinator.handlerName)

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.keyboardDismissMode = .interactive
        webView.isOpaque = false
        context.coordinator.webView = webView
        session.webView = webView
        webView.load(URLRequest(url: url, cachePolicy: .useProtocolCachePolicy))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.launchURL = url
        if webView.url == nil, !webView.isLoading {
            webView.load(URLRequest(url: url))
        }
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.stopLoading()
        webView.configuration.userContentController.removeScriptMessageHandler(forName: Coordinator.handlerName)
        webView.navigationDelegate = nil
        webView.uiDelegate = nil
        coordinator.session.webView = nil
        coordinator.webView = nil
    }

    @MainActor
    final class Coordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate, WKUIDelegate {
        static let handlerName = "observer"

        var launchURL: URL
        let session: WebViewSession
        weak var webView: WKWebView?
        private let onEvent: (Any) -> Void
        private let onLoadingChanged: (Bool) -> Void
        private let onError: (String) -> Void

        init(
            launchURL: URL,
            session: WebViewSession,
            onEvent: @escaping (Any) -> Void,
            onLoadingChanged: @escaping (Bool) -> Void,
            onError: @escaping (String) -> Void
        ) {
            self.launchURL = launchURL
            self.session = session
            self.onEvent = onEvent
            self.onLoadingChanged = onLoadingChanged
            self.onError = onError
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.name == Self.handlerName else { return }
            onEvent(message.body)
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation?) {
            onLoadingChanged(true)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation?) {
            onLoadingChanged(false)
            session.canGoBack = webView.canGoBack
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation?, withError error: Error) {
            report(error)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation?, withError error: Error) {
            report(error)
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping @MainActor @Sendable (WKNavigationActionPolicy) -> Void
        ) {
            guard navigationAction.targetFrame?.isMainFrame != false,
                  let candidate = navigationAction.request.url else {
                decisionHandler(.allow)
                return
            }
            if candidate == launchURL || URLPolicy.isAllowedNavigation(launchURL: launchURL, candidate: candidate) {
                decisionHandler(.allow)
            } else {
                decisionHandler(.cancel)
                UIApplication.shared.open(candidate)
            }
        }

        private func report(_ error: Error) {
            onLoadingChanged(false)
            if (error as NSError).code != NSURLErrorCancelled { onError(error.localizedDescription) }
        }
    }
}
