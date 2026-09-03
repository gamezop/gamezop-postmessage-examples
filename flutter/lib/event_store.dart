import 'package:flutter/foundation.dart';
import 'game_event.dart';

class EventStore extends ChangeNotifier {
  EventStore({this.capacity = 500});
  final int capacity;
  final List<CapturedGameEvent> _events = [];
  int _nextId = 1;
  int? selectedEventId;
  int droppedEventCount = 0;

  List<CapturedGameEvent> get events => List.unmodifiable(_events);
  CapturedGameEvent? get selectedEvent {
    if (_events.isEmpty) return null;
    for (final event in _events) {
      if (event.id == selectedEventId) return event;
    }
    return _events.last;
  }

  void capture(dynamic body, {DateTime? receivedAt}) {
    final event = GameEventParser.parse(
      body,
      id: _nextId++,
      receivedAt: receivedAt,
    );
    _events.add(event);
    if (_events.length > capacity) {
      final overflow = _events.length - capacity;
      _events.removeRange(0, overflow);
      droppedEventCount += overflow;
    }
    selectedEventId = event.id;
    notifyListeners();
  }

  void select(int id) {
    selectedEventId = id;
    notifyListeners();
  }

  void clear() {
    _events.clear();
    selectedEventId = null;
    droppedEventCount = 0;
    notifyListeners();
  }
}
