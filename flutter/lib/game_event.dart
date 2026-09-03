import 'dart:convert';

enum GameMode { individual, battles }

extension GameModeLabel on GameMode {
  String get label => this == GameMode.individual ? 'Individual' : 'Battles';
}

enum EventFamily { individual, battles, unknown }

enum ValidationStatus { valid, unknown, malformed }

class CapturedGameEvent {
  const CapturedGameEvent({
    required this.id,
    required this.receivedAt,
    required this.rawJson,
    required this.prettyJson,
    required this.family,
    required this.name,
    required this.fields,
    required this.status,
  });
  final int id;
  final DateTime receivedAt;
  final String rawJson;
  final String prettyJson;
  final EventFamily family;
  final String name;
  final Map<String, dynamic> fields;
  final ValidationStatus status;
}

class GameEventParser {
  static const individualStates = {'loaded', 'start', 'playing', 'over', 'levelup'};
  static const battlesEvents = {
    'match_found',
    'match_not_found',
    'match_start',
    'match_playing',
    'match_over',
    'match_result',
    'go_home',
  };

  static CapturedGameEvent parse(
    dynamic body, {
    required int id,
    DateTime? receivedAt,
  }) {
    final timestamp = receivedAt ?? DateTime.now();
    String raw;
    try {
      final dynamic decoded;
      if (body is String) {
        raw = body;
        decoded = jsonDecode(body);
      } else {
        raw = jsonEncode(body);
        decoded = body;
      }
      if (decoded is! Map) {
        throw const FormatException('Payload is not a JSON object');
      }
      final fields = decoded.map(
        (key, value) => MapEntry(key.toString(), value),
      );
      final state = fields['state'];
      final event = fields['event'];
      if (state is String) {
        return CapturedGameEvent(
          id: id,
          receivedAt: timestamp,
          rawJson: raw,
          prettyJson: const JsonEncoder.withIndent('  ').convert(fields),
          family: EventFamily.individual,
          name: state,
          fields: fields,
          status:
              individualStates.contains(state)
                  ? ValidationStatus.valid
                  : ValidationStatus.unknown,
        );
      }
      if (event is String) {
        return CapturedGameEvent(
          id: id,
          receivedAt: timestamp,
          rawJson: raw,
          prettyJson: const JsonEncoder.withIndent('  ').convert(fields),
          family: EventFamily.battles,
          name: event,
          fields: fields,
          status:
              battlesEvents.contains(event)
                  ? ValidationStatus.valid
                  : ValidationStatus.unknown,
        );
      }
      return CapturedGameEvent(
        id: id,
        receivedAt: timestamp,
        rawJson: raw,
        prettyJson: const JsonEncoder.withIndent('  ').convert(fields),
        family: EventFamily.unknown,
        name: 'unknown_payload',
        fields: fields,
        status: ValidationStatus.unknown,
      );
    } catch (error) {
      raw = body is String ? body : body.toString();
      return CapturedGameEvent(
        id: id,
        receivedAt: timestamp,
        rawJson: raw,
        prettyJson: raw,
        family: EventFamily.unknown,
        name: 'malformed_json',
        fields: {'error': error.toString()},
        status: ValidationStatus.malformed,
      );
    }
  }
}
