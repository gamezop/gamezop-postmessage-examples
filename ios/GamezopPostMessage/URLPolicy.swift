import Foundation

enum URLPolicy {
    static func validationError(_ value: String, mode: GameMode) -> String? {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "Enter a Gamezop HTTPS URL" }
        guard let components = URLComponents(string: trimmed) else { return "Enter a valid HTTPS URL" }
        guard components.scheme?.lowercased() == "https" else { return "Only HTTPS URLs are accepted" }
        guard components.host?.isEmpty == false else { return "The URL must include a host" }
        guard components.user == nil, components.password == nil else {
            return "URLs containing embedded credentials are not accepted"
        }
        guard mode == .battles else { return nil }
        return roomDetailsError(components)
    }

    static func isAllowedNavigation(launchURL: URL, candidate: URL) -> Bool {
        guard candidate.scheme?.lowercased() == "https", let candidateHost = candidate.host?.lowercased() else {
            return false
        }
        return candidateHost == launchURL.host?.lowercased() || isDocumentedGamezopHost(candidateHost)
    }

    static func isDocumentedGamezopHost(_ host: String) -> Bool {
        ["gamezop.com", "umogames.com"].contains { host == $0 || host.hasSuffix(".\($0)") }
    }

    private static func roomDetailsError(_ components: URLComponents) -> String? {
        let values = (components.queryItems ?? []).filter { $0.name == "roomDetails" }
        guard !values.isEmpty else { return "Battles URL must include roomDetails" }
        guard values.count == 1 else { return "Battles URL must include roomDetails only once" }
        guard let encoded = values[0].value, !encoded.isEmpty else { return "roomDetails cannot be empty" }

        var normalized = encoded.replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        normalized += String(repeating: "=", count: (4 - normalized.count % 4) % 4)
        guard let data = Data(base64Encoded: normalized),
              let object = try? JSONSerialization.jsonObject(with: data),
              let dictionary = object as? [String: Any], !dictionary.isEmpty else {
            return Data(base64Encoded: normalized) == nil
                ? "roomDetails must be valid Base64"
                : "Decoded roomDetails must be a non-empty JSON object"
        }
        return nil
    }
}
