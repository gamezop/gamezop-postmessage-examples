import SwiftUI

struct LaunchRequest: Identifiable {
    let id = UUID()
    let mode: GameMode
    let url: URL
}

struct ContentView: View {
    @StateObject private var store = EventStore()
    @State private var launch: LaunchRequest?

    var body: some View {
        Group {
            if let launch {
                GameScreen(launch: launch, store: store) { self.launch = nil }
            } else {
                LauncherScreen { mode, url in
                    launch = LaunchRequest(mode: mode, url: url)
                }
            }
        }
    }
}

private struct LauncherScreen: View {
    let onLaunch: (GameMode, URL) -> Void
    @State private var mode = GameMode.individual
    @State private var individualURL = ""
    @State private var battlesURL = ""
    @State private var validationError: String?

    private var currentURL: Binding<String> {
        mode == .individual ? $individualURL : $battlesURL
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Gamezop Event Bridge")
                        .font(.largeTitle.bold())
                    Text("Launch a real Gamezop URL and inspect native bridge events in real time.")
                        .foregroundStyle(.secondary)
                    Picker("Mode", selection: $mode) {
                        ForEach(GameMode.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .accessibilityIdentifier("mode-picker")

                    VStack(alignment: .leading, spacing: 12) {
                        Text(mode == .individual ? "Individual game URL" : "Final Battles URL")
                            .font(.headline)
                        Text(mode == .individual
                             ? "Paste a Gamezop game URL or Unique Link."
                             : "Paste the generated Battles URL containing roomDetails.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        TextField("https://…", text: currentURL)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.URL)
                            .textFieldStyle(.roundedBorder)
                            .accessibilityIdentifier("url-input")
                            .onChange(of: currentURL.wrappedValue) { _ in validationError = nil }
                        if let validationError {
                            Text(validationError).font(.caption).foregroundStyle(.red)
                                .accessibilityIdentifier("validation-error")
                        }
                        Button("Launch \(mode.label)") {
                            let value = currentURL.wrappedValue
                            if let error = URLPolicy.validationError(value, mode: mode) {
                                validationError = error
                            } else if let url = URL(string: value.trimmingCharacters(in: .whitespacesAndNewlines)) {
                                onLaunch(mode, url)
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("launch-game")
                    }
                    .padding()
                    .background(.background, in: RoundedRectangle(cornerRadius: 20))
                    .shadow(color: .black.opacity(0.08), radius: 12, y: 4)

                    Text("Events remain in memory and are cleared when the app process ends.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(24)
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarHidden(true)
        }
        .navigationViewStyle(.stack)
    }
}

private struct GameScreen: View {
    let launch: LaunchRequest
    @ObservedObject var store: EventStore
    let onClose: () -> Void

    @StateObject private var webSession = WebViewSession()
    @State private var loading = true
    @State private var errorMessage: String?
    @State private var showLogs = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                Button(action: handleBack) { Image(systemName: "xmark") }
                    .buttonStyle(.borderless)
                    .accessibilityLabel("Close game")
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(launch.mode.label) game").font(.subheadline.bold())
                    Text(loading ? "Loading…" : "observer ready").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Button { errorMessage = nil; webSession.reload() } label: { Image(systemName: "arrow.clockwise") }
                    .buttonStyle(.borderless)
                    .accessibilityLabel("Reload game")
            }
            .padding(.horizontal, 14)
            .frame(height: 56)
            .background(.bar)

            GeometryReader { geometry in
                ZStack {
                    GameWebView(
                        url: launch.url,
                        session: webSession,
                        onEvent: { store.capture($0) },
                        onLoadingChanged: { loading = $0 },
                        onError: { errorMessage = $0 }
                    )

                    if showLogs {
                        EventInspector(store: store)
                            .frame(height: min(300, max(170, geometry.size.height * 0.44)))
                            .frame(maxHeight: .infinity, alignment: .bottom)
                            .transition(.move(edge: .bottom))
                    }

                    DraggableLogPill(
                        size: geometry.size,
                        count: store.events.count,
                        visible: showLogs,
                        action: { withAnimation(.easeOut(duration: 0.2)) { showLogs.toggle() } }
                    )

                    if let errorMessage {
                        VStack(spacing: 12) {
                            Text("Could not load page").font(.headline)
                            Text(errorMessage).font(.caption).foregroundStyle(.secondary)
                            Button("Try again") { self.errorMessage = nil; webSession.reload() }
                        }
                        .padding(20)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
                        .padding(24)
                    }
                }
            }
        }
        .background(Color(.systemBackground))
    }

    private func handleBack() {
        if webSession.canGoBack { webSession.goBack() } else { onClose() }
    }
}

private struct DraggableLogPill: View {
    let size: CGSize
    let count: Int
    let visible: Bool
    let action: () -> Void
    @State private var position: CGPoint?

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Text(visible ? "Hide" : "Logs").font(.subheadline.bold())
                Text("\(count)").font(.caption.bold())
                    .padding(.horizontal, 7).padding(.vertical, 3)
                    .background(Color.white.opacity(0.12), in: Capsule())
            }
            .foregroundStyle(Color.cyan)
            .frame(width: 92, height: 48)
            .background(Color(red: 0.07, green: 0.10, blue: 0.18).opacity(0.94), in: Capsule())
            .overlay(Capsule().stroke(Color.cyan.opacity(0.35)))
            .shadow(radius: 8)
        }
        .accessibilityIdentifier("event-log-toggle")
        .position(position ?? CGPoint(x: max(58, size.width - 58), y: 36))
        .highPriorityGesture(
            DragGesture(minimumDistance: 8)
                .onChanged { value in
                    position = CGPoint(
                        x: min(max(46, value.location.x), max(46, size.width - 46)),
                        y: min(max(24, value.location.y), max(24, size.height - 24))
                    )
                }
        )
        .onChange(of: size) { newSize in
            guard let point = position else { return }
            position = CGPoint(
                x: min(max(46, point.x), max(46, newSize.width - 46)),
                y: min(max(24, point.y), max(24, newSize.height - 24))
            )
        }
    }
}

private struct EventInspector: View {
    @ObservedObject var store: EventStore

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Circle().fill(store.events.isEmpty ? Color.gray : Color.green).frame(width: 10, height: 10)
                VStack(alignment: .leading) {
                    Text("Event logs").font(.headline)
                    Text("\(store.events.count) captured events").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Button("Clear", action: store.clear).disabled(store.events.isEmpty)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            Divider()
            if store.events.isEmpty {
                VStack(spacing: 5) {
                    Text("Waiting for Gamezop events").font(.subheadline.bold())
                    Text("The message handler is registered as observer.").font(.caption).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                HStack(spacing: 0) {
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(store.events.reversed()) { event in
                                Button {
                                    store.selectedEventID = event.id
                                } label: {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(event.name).font(.caption.bold()).lineLimit(1)
                                        Text(event.family.rawValue).font(.caption2).foregroundStyle(.secondary)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(8)
                                    .background(eventColor(event).opacity(0.13), in: RoundedRectangle(cornerRadius: 9))
                                }
                                .buttonStyle(.plain)
                            }
                        }.padding(10)
                    }
                    .frame(maxWidth: 150)
                    Divider()
                    if let event = store.selectedEvent {
                        EventDetails(event: event)
                    }
                }
            }
        }
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(radius: 12)
        .padding(.horizontal, 8)
        .padding(.bottom, 8)
        .accessibilityIdentifier("event-log-panel")
    }
}

private struct EventDetails: View {
    let event: CapturedGameEvent

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(event.name).font(.headline)
                        Text(event.status.rawValue).font(.caption).foregroundStyle(eventColor(event))
                    }
                    Spacer()
                    Button("Copy JSON") { UIPasteboard.general.string = event.rawJSON }
                }
                ForEach(event.fields.keys.sorted(), id: \.self) { key in
                    Text(key).font(.caption.bold())
                    Text(event.fields[key]?.displayValue ?? "null").font(.caption.monospaced())
                }
                Text("Raw JSON").font(.caption.bold())
                Text(event.prettyJSON).font(.caption2.monospaced()).textSelection(.enabled)
                    .padding(8).background(Color.secondary.opacity(0.1), in: RoundedRectangle(cornerRadius: 8))
            }.padding(10)
        }
    }
}

private func eventColor(_ event: CapturedGameEvent) -> Color {
    if event.status == .malformed { return .red }
    if event.status == .unknown { return .orange }
    return event.family == .battles ? .purple : .teal
}
