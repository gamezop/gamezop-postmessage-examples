import Foundation

enum GameMode: String, CaseIterable, Identifiable {
    case individual
    case battles

    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum EventFamily: String {
    case individual
    case battles
    case unknown
}

enum ValidationStatus: String {
    case valid
    case unknown
    case malformed
}

enum JSONValue: Equatable, Sendable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case object([String: JSONValue])
    case array([JSONValue])
    case null

    init(_ value: Any) {
        switch value {
        case let value as String: self = .string(value)
        case let value as NSNumber:
            self = CFGetTypeID(value) == CFBooleanGetTypeID() ? .bool(value.boolValue) : .number(value.doubleValue)
        case let value as [String: Any]: self = .object(value.mapValues(JSONValue.init))
        case let value as [Any]: self = .array(value.map(JSONValue.init))
        default: self = .null
        }
    }

    var displayValue: String {
        switch self {
        case let .string(value): return value
        case let .number(value): return value.rounded() == value ? String(Int64(value)) : String(value)
        case let .bool(value): return String(value)
        case .null: return "null"
        case .object, .array: return prettyPrinted
        }
    }

    var foundationValue: Any {
        switch self {
        case let .string(value): return value
        case let .number(value): return value
        case let .bool(value): return value
        case let .object(value): return value.mapValues(\.foundationValue)
        case let .array(value): return value.map(\.foundationValue)
        case .null: return NSNull()
        }
    }

    var prettyPrinted: String {
        guard JSONSerialization.isValidJSONObject(foundationValue),
              let data = try? JSONSerialization.data(withJSONObject: foundationValue, options: [.prettyPrinted, .sortedKeys]),
              let output = String(data: data, encoding: .utf8) else { return displayValue }
        return output
    }
}

struct CapturedGameEvent: Identifiable, Equatable, Sendable {
    let id: Int64
    let receivedAt: Date
    let rawJSON: String
    let prettyJSON: String
    let family: EventFamily
    let name: String
    let fields: [String: JSONValue]
    let status: ValidationStatus
}

enum GameEventParser {
    static let individualStates = Set(["loaded", "start", "playing", "over", "levelup"])
    static let battlesEvents = Set([
        "match_found", "match_not_found", "match_start", "match_playing",
        "match_over", "match_result", "go_home",
    ])

    static func parse(body: Any, id: Int64, receivedAt: Date = Date()) -> CapturedGameEvent {
        let raw: String
        let jsonObject: Any

        do {
            if let string = body as? String {
                raw = string
                guard let data = string.data(using: .utf8) else { throw ParseError.invalidUTF8 }
                jsonObject = try JSONSerialization.jsonObject(with: data)
            } else {
                guard JSONSerialization.isValidJSONObject(body) else { throw ParseError.notJSONObject }
                let data = try JSONSerialization.data(withJSONObject: body, options: [.sortedKeys])
                raw = String(decoding: data, as: UTF8.self)
                jsonObject = body
            }

            guard let dictionary = jsonObject as? [String: Any] else { throw ParseError.notJSONObject }
            let fields = dictionary.mapValues(JSONValue.init)
            let prettyData = try JSONSerialization.data(withJSONObject: dictionary, options: [.prettyPrinted, .sortedKeys])
            let pretty = String(decoding: prettyData, as: UTF8.self)

            if let state = dictionary["state"] as? String {
                return CapturedGameEvent(
                    id: id, receivedAt: receivedAt, rawJSON: raw, prettyJSON: pretty,
                    family: .individual, name: state, fields: fields,
                    status: individualStates.contains(state) ? .valid : .unknown
                )
            }
            if let event = dictionary["event"] as? String {
                return CapturedGameEvent(
                    id: id, receivedAt: receivedAt, rawJSON: raw, prettyJSON: pretty,
                    family: .battles, name: event, fields: fields,
                    status: battlesEvents.contains(event) ? .valid : .unknown
                )
            }
            return CapturedGameEvent(
                id: id, receivedAt: receivedAt, rawJSON: raw, prettyJSON: pretty,
                family: .unknown, name: "unknown_payload", fields: fields, status: .unknown
            )
        } catch {
            let fallback = body as? String ?? String(describing: body)
            return CapturedGameEvent(
                id: id, receivedAt: receivedAt, rawJSON: fallback, prettyJSON: fallback,
                family: .unknown, name: "malformed_json",
                fields: ["error": .string(error.localizedDescription)], status: .malformed
            )
        }
    }

    private enum ParseError: LocalizedError {
        case invalidUTF8
        case notJSONObject

        var errorDescription: String? {
            switch self {
            case .invalidUTF8: return "Payload is not valid UTF-8"
            case .notJSONObject: return "Payload is not a JSON object"
            }
        }
    }
}
