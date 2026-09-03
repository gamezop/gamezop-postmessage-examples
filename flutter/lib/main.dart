import 'package:flutter/material.dart';
import 'event_store.dart';
import 'game_event.dart';
import 'game_screen.dart';
import 'launcher_screen.dart';

void main() => runApp(const GamezopPostMessageApp());

class GamezopPostMessageApp extends StatefulWidget {
  const GamezopPostMessageApp({super.key});
  @override
  State<GamezopPostMessageApp> createState() => _GamezopPostMessageAppState();
}

class _GamezopPostMessageAppState extends State<GamezopPostMessageApp> {
  final EventStore store = EventStore();
  GameMode? mode;
  Uri? activeUri;
  @override
  void dispose() {
    store.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Gamezop PostMessage',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      colorSchemeSeed: const Color(0xff5b4cf0),
      useMaterial3: true,
    ),
    darkTheme: ThemeData(
      colorSchemeSeed: const Color(0xffc8bfff),
      brightness: Brightness.dark,
      useMaterial3: true,
    ),
    home:
        activeUri == null
            ? LauncherScreen(
              onLaunch:
                  (selectedMode, uri) => setState(() {
                    mode = selectedMode;
                    activeUri = uri;
                  }),
            )
            : GameScreen(
              key: ValueKey(activeUri),
              uri: activeUri!,
              mode: mode!,
              store: store,
              onClose:
                  () => setState(() {
                    activeUri = null;
                  }),
            ),
  );
}
