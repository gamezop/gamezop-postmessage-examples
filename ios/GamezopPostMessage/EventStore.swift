import Foundation

@MainActor
final class EventStore: ObservableObject {
    @Published private(set) var events: [CapturedGameEvent] = []
    @Published var selectedEventID: Int64?
    @Published private(set) var droppedEventCount = 0

    private var nextID: Int64 = 1
    private let capacity: Int

    init(capacity: Int = 500) {
        self.capacity = capacity
    }

    func capture(_ body: Any, at date: Date = Date()) {
        let event = GameEventParser.parse(body: body, id: nextID, receivedAt: date)
        nextID += 1
        events.append(event)
        if events.count > capacity {
            let overflow = events.count - capacity
            events.removeFirst(overflow)
            droppedEventCount += overflow
        }
        selectedEventID = event.id
    }

    func clear() {
        events.removeAll(keepingCapacity: true)
        selectedEventID = nil
        droppedEventCount = 0
    }

    var selectedEvent: CapturedGameEvent? {
        events.first { $0.id == selectedEventID } ?? events.last
    }
}
