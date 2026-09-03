import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'event_store.dart';
import 'game_event.dart';
import 'url_policy.dart';

class GameScreen extends StatefulWidget {
  const GameScreen({
    super.key,
    required this.uri,
    required this.mode,
    required this.store,
    required this.onClose,
  });
  final Uri uri;
  final GameMode mode;
  final EventStore store;
  final VoidCallback onClose;
  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen> {
  late final WebViewController controller;
  bool loading = true;
  bool showLogs = false;
  String? error;
  Offset pill = const Offset(12, 12);

  @override
  void initState() {
    super.initState();
    controller =
        WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(Colors.transparent)
          ..addJavaScriptChannel(
            'FlutterWebView',
            onMessageReceived:
                (message) => widget.store.capture(message.message),
          )
          ..setNavigationDelegate(
            NavigationDelegate(
              onPageStarted:
                  (_) => setState(() {
                    loading = true;
                    error = null;
                  }),
              onPageFinished: (_) => setState(() => loading = false),
              onWebResourceError: (webError) {
                if (webError.isForMainFrame ?? true) {
                  setState(() {
                    loading = false;
                    error = webError.description;
                  });
                }
              },
              onNavigationRequest: (request) {
                final candidate = Uri.tryParse(request.url);
                if (candidate != null &&
                    UrlPolicy.isAllowedNavigation(widget.uri, candidate)) {
                  return NavigationDecision.navigate;
                }
                if (candidate != null) {
                  launchUrl(candidate, mode: LaunchMode.externalApplication);
                }
                return NavigationDecision.prevent;
              },
            ),
          )
          ..loadRequest(widget.uri);
  }

  Future<void> handleBack() async {
    if (await controller.canGoBack()) {
      await controller.goBack();
    } else {
      widget.onClose();
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: false,
    onPopInvokedWithResult: (_, _) => handleBack(),
    child: Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Material(
              elevation: 3,
              child: SizedBox(
                height: 58,
                child: Row(
                  children: [
                    IconButton(
                      key: const Key('close-game'),
                      onPressed: handleBack,
                      icon: const Icon(Icons.close),
                      tooltip: 'Close game',
                    ),
                    Expanded(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${widget.mode.label} game',
                            style: const TextStyle(fontWeight: FontWeight.w600),
                          ),
                          Text(
                            loading ? 'Loading…' : 'FlutterWebView ready',
                            style: Theme.of(context).textTheme.labelSmall,
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      key: const Key('reload-game'),
                      onPressed: () {
                        setState(() => error = null);
                        controller.reload();
                      },
                      icon: const Icon(Icons.refresh),
                      tooltip: 'Reload game',
                    ),
                  ],
                ),
              ),
            ),
            Expanded(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  const pillSize = Size(92, 48);
                  final maxX = (constraints.maxWidth - pillSize.width).clamp(
                    0.0,
                    double.infinity,
                  );
                  final maxY = (constraints.maxHeight - pillSize.height).clamp(
                    0.0,
                    double.infinity,
                  );
                  pill = Offset(
                    pill.dx.clamp(0.0, maxX).toDouble(),
                    pill.dy.clamp(0.0, maxY).toDouble(),
                  );
                  return Stack(
                    children: [
                      Positioned.fill(
                        child: WebViewWidget(controller: controller),
                      ),
                      if (showLogs)
                        Positioned(
                          left: 8,
                          right: 8,
                          bottom: 8,
                          height:
                              (constraints.maxHeight * .44)
                                  .clamp(170.0, 300.0)
                                  .toDouble(),
                          child: EventInspector(store: widget.store),
                        ),
                      Positioned(
                        left: pill.dx,
                        top: pill.dy,
                        child: GestureDetector(
                          onPanUpdate:
                              (details) => setState(
                                () =>
                                    pill = Offset(
                                      (pill.dx + details.delta.dx)
                                          .clamp(0.0, maxX)
                                          .toDouble(),
                                      (pill.dy + details.delta.dy)
                                          .clamp(0.0, maxY)
                                          .toDouble(),
                                    ),
                              ),
                          child: FilledButton(
                            key: const Key('event-log-toggle'),
                            style: FilledButton.styleFrom(
                              fixedSize: pillSize,
                              backgroundColor: const Color(0xee111a2e),
                              foregroundColor: const Color(0xff67e8f9),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 10,
                              ),
                            ),
                            onPressed:
                                () => setState(() => showLogs = !showLogs),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  showLogs ? 'Hide' : 'Logs',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(width: 7),
                                ListenableBuilder(
                                  listenable: widget.store,
                                  builder:
                                      (_, _) => Badge(
                                        label: Text(
                                          '${widget.store.events.length}',
                                        ),
                                      ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      if (error != null)
                        Center(
                          child: Card(
                            child: Padding(
                              padding: const EdgeInsets.all(20),
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Text(
                                    'Could not load page',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(error!),
                                  const SizedBox(height: 12),
                                  FilledButton(
                                    onPressed: () {
                                      setState(() => error = null);
                                      controller.reload();
                                    },
                                    child: const Text('Try again'),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class EventInspector extends StatelessWidget {
  const EventInspector({super.key, required this.store});
  final EventStore store;
  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) {
      final selected = store.selectedEvent;
      return Material(
        key: const Key('event-log-panel'),
        elevation: 12,
        borderRadius: BorderRadius.circular(20),
        clipBehavior: Clip.antiAlias,
        child: Column(
          children: [
            SizedBox(
              height: 60,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 14),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 5,
                      backgroundColor:
                          store.events.isEmpty ? Colors.grey : Colors.green,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Event logs',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                          Text(
                            '${store.events.length} captured events${store.droppedEventCount > 0 ? ' · ${store.droppedEventCount} discarded' : ''}',
                            style: Theme.of(context).textTheme.labelSmall,
                          ),
                        ],
                      ),
                    ),
                    OutlinedButton(
                      key: const Key('clear-events'),
                      onPressed: store.events.isEmpty ? null : store.clear,
                      child: const Text('Clear'),
                    ),
                  ],
                ),
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child:
                  store.events.isEmpty
                      ? const Center(
                        child: Text(
                          'Waiting for FlutterWebView.postMessage events',
                        ),
                      )
                      : Row(
                        children: [
                          SizedBox(
                            width: 145,
                            child: ListView(
                              padding: const EdgeInsets.all(8),
                              children:
                                  store.events.reversed
                                      .map(
                                        (event) => EventTile(
                                          event: event,
                                          selected: event.id == selected?.id,
                                          onTap: () => store.select(event.id),
                                        ),
                                      )
                                      .toList(),
                            ),
                          ),
                          const VerticalDivider(width: 1),
                          if (selected != null)
                            Expanded(child: EventDetails(event: selected)),
                        ],
                      ),
            ),
          ],
        ),
      );
    },
  );
}

class EventTile extends StatelessWidget {
  const EventTile({
    super.key,
    required this.event,
    required this.selected,
    required this.onTap,
  });
  final CapturedGameEvent event;
  final bool selected;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) {
    final color = eventColor(event);
    return Card(
      margin: const EdgeInsets.only(bottom: 7),
      color: color.withValues(alpha: .13),
      shape: RoundedRectangleBorder(
        side: selected ? BorderSide(color: color, width: 2) : BorderSide.none,
        borderRadius: BorderRadius.circular(9),
      ),
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                event.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontWeight: FontWeight.bold, color: color),
              ),
              Text(
                event.family.name,
                style: Theme.of(context).textTheme.labelSmall,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class EventDetails extends StatelessWidget {
  const EventDetails({super.key, required this.event});
  final CapturedGameEvent event;
  @override
  Widget build(BuildContext context) {
    final keys = event.fields.keys.toList()..sort();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      event.name,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    Text(
                      event.status.name,
                      style: TextStyle(color: eventColor(event)),
                    ),
                  ],
                ),
              ),
              OutlinedButton(
                onPressed:
                    () => Clipboard.setData(ClipboardData(text: event.rawJson)),
                child: const Text('Copy JSON'),
              ),
            ],
          ),
          for (final key in keys) ...[
            const SizedBox(height: 8),
            Text(
              key,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 11),
            ),
            SelectableText(
              formatValue(event.fields[key]),
              style: const TextStyle(fontSize: 11),
            ),
          ],
          const SizedBox(height: 10),
          const Text('Raw JSON', style: TextStyle(fontWeight: FontWeight.bold)),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(9),
            ),
            child: SelectableText(
              event.prettyJson,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
            ),
          ),
        ],
      ),
    );
  }
}

String formatValue(dynamic value) =>
    value is Map || value is List
        ? const JsonEncoder.withIndent('  ').convert(value)
        : value?.toString() ?? 'null';
Color eventColor(CapturedGameEvent event) {
  if (event.status == ValidationStatus.malformed) return Colors.red;
  if (event.status == ValidationStatus.unknown) return Colors.orange;
  return event.family == EventFamily.battles ? Colors.purple : Colors.teal;
}
