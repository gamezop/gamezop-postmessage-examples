import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:gamezop_postmessage_flutter/event_store.dart';
import 'package:gamezop_postmessage_flutter/game_event.dart';
import 'package:gamezop_postmessage_flutter/url_policy.dart';

void main() {
  group('GameEventParser', () {
    for (final state in GameEventParser.individualStates) {
      test('parses individual $state', () {
        final event = GameEventParser.parse({
          'state': state,
          'score': 10,
          'futureField': true,
        }, id: 1);
        expect(event.family, EventFamily.individual);
        expect(event.status, ValidationStatus.valid);
        expect(event.fields['futureField'], isTrue);
      });
    }

    for (final name in GameEventParser.battlesEvents) {
      test('parses Battles $name', () {
        final event = GameEventParser.parse(
          jsonEncode({'event': name, 'roomId': 'room-1'}),
          id: 1,
        );
        expect(event.family, EventFamily.battles);
        expect(event.status, ValidationStatus.valid);
      });
    }

    test('preserves unknown and malformed payloads', () {
      expect(
        GameEventParser.parse({'state': 'future'}, id: 1).status,
        ValidationStatus.unknown,
      );
      expect(
        GameEventParser.parse('{broken', id: 2).status,
        ValidationStatus.malformed,
      );
    });
  });

  test('EventStore keeps newest 500 events and clears', () {
    final store = EventStore();
    for (var index = 0; index < 505; index++) {
      store.capture({'state': 'playing', 'score': index});
    }
    expect(store.events, hasLength(500));
    expect(store.events.first.fields['score'], 5);
    expect(store.droppedEventCount, 5);
    store.clear();
    expect(store.events, isEmpty);
  });

  group('UrlPolicy', () {
    String battlesUrl(Object details) {
      final encoded = base64Url
          .encode(utf8.encode(jsonEncode(details)))
          .replaceAll('=', '');
      return 'https://11353.play.gamezop.com/g/example?roomDetails=$encoded';
    }

    test('accepts HTTPS and valid URL-safe Base64 roomDetails', () {
      expect(
        UrlPolicy.validationError(
          'https://11353.play.gamezop.com/g/example',
          GameMode.individual,
        ),
        isNull,
      );
      expect(
        UrlPolicy.validationError(
          battlesUrl({'roomId': 'ABC01'}),
          GameMode.battles,
        ),
        isNull,
      );
    });

    test('rejects credentials and invalid Battles URLs', () {
      expect(
        UrlPolicy.validationError('https://user@example.com', GameMode.individual),
        isNotNull,
      );
      expect(
        UrlPolicy.validationError('https://example.com', GameMode.battles),
        isNotNull,
      );
      expect(
        UrlPolicy.validationError(
          'https://example.com?roomDetails=bad',
          GameMode.battles,
        ),
        isNotNull,
      );
      expect(
        UrlPolicy.validationError(
          '${battlesUrl({'x': 1})}&roomDetails=again',
          GameMode.battles,
        ),
        isNotNull,
      );
    });
  });
}
