import 'dart:convert';
import 'game_event.dart';

class UrlPolicy {
  static String? validationError(String value, GameMode mode) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return 'Enter a Gamezop HTTPS URL';
    final uri = Uri.tryParse(trimmed);
    if (uri == null) return 'Enter a valid HTTPS URL';
    if (uri.scheme.toLowerCase() != 'https') {
      return 'Only HTTPS URLs are accepted';
    }
    if (uri.host.isEmpty) return 'The URL must include a host';
    if (uri.userInfo.isNotEmpty) {
      return 'URLs containing embedded credentials are not accepted';
    }
    if (mode != GameMode.battles) return null;
    final values = uri.queryParametersAll['roomDetails'] ?? const [];
    if (values.isEmpty) return 'Battles URL must include roomDetails';
    if (values.length > 1) {
      return 'Battles URL must include roomDetails only once';
    }
    if (values.single.isEmpty) return 'roomDetails cannot be empty';
    try {
      final bytes = base64Url.decode(base64Url.normalize(values.single));
      final decoded = jsonDecode(utf8.decode(bytes));
      if (decoded is! Map || decoded.isEmpty) {
        return 'Decoded roomDetails must be a non-empty JSON object';
      }
    } catch (_) {
      return 'roomDetails must be valid Base64 JSON';
    }
    return null;
  }

  static bool isAllowedNavigation(Uri launchUri, Uri candidate) {
    if (candidate.scheme.toLowerCase() != 'https' || candidate.host.isEmpty) {
      return false;
    }
    final host = candidate.host.toLowerCase();
    return host == launchUri.host.toLowerCase() ||
        isDocumentedGamezopHost(host);
  }

  static bool isDocumentedGamezopHost(String host) => const [
    'gamezop.com',
    'umogames.com',
  ].any((domain) => host == domain || host.endsWith('.$domain'));
}
