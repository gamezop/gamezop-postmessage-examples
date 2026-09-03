import 'package:flutter/material.dart';
import 'game_event.dart';
import 'url_policy.dart';

class LauncherScreen extends StatefulWidget {
  const LauncherScreen({super.key, required this.onLaunch});
  final void Function(GameMode mode, Uri uri) onLaunch;
  @override
  State<LauncherScreen> createState() => _LauncherScreenState();
}

class _LauncherScreenState extends State<LauncherScreen> {
  GameMode mode = GameMode.individual;
  final controllers = {
    GameMode.individual: TextEditingController(),
    GameMode.battles: TextEditingController(),
  };
  String? error;
  @override
  void dispose() {
    for (final controller in controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  void launch() {
    final value = controllers[mode]!.text;
    final result = UrlPolicy.validationError(value, mode);
    setState(() => error = result);
    if (result == null) widget.onLaunch(mode, Uri.parse(value.trim()));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 680),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Gamezop Event Bridge',
                  style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Launch a real Gamezop URL and inspect bridge events in real time.',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 28),
                SegmentedButton<GameMode>(
                  key: const Key('mode-tabs'),
                  segments:
                      GameMode.values
                          .map(
                            (value) => ButtonSegment(
                              value: value,
                              label: Text(value.label),
                            ),
                          )
                          .toList(),
                  selected: {mode},
                  onSelectionChanged:
                      (values) => setState(() {
                        mode = values.single;
                        error = null;
                      }),
                ),
                const SizedBox(height: 20),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          mode == GameMode.individual
                              ? 'Individual game URL'
                              : 'Final Battles URL',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          mode == GameMode.individual
                              ? 'Paste a Gamezop game URL or Unique Link.'
                              : 'Paste the generated Battles URL containing roomDetails.',
                        ),
                        const SizedBox(height: 14),
                        TextField(
                          key: const Key('url-input'),
                          controller: controllers[mode],
                          keyboardType: TextInputType.url,
                          autocorrect: false,
                          textCapitalization: TextCapitalization.none,
                          decoration: InputDecoration(
                            labelText: 'https://…',
                            errorText: error,
                            border: const OutlineInputBorder(),
                          ),
                          onChanged: (_) {
                            if (error != null) setState(() => error = null);
                          },
                          onSubmitted: (_) => launch(),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            key: const Key('launch-game'),
                            onPressed: launch,
                            child: Text('Launch ${mode.label}'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  'Events remain in memory and are cleared when the app process ends.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}
