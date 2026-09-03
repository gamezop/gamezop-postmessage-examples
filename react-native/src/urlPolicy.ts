import {Base64} from 'js-base64';
import type {GameMode} from './events';

export function validateLaunchUrl(value: string, mode: GameMode): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return 'Enter a Gamezop HTTPS URL';
  }
  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    return 'Enter a valid HTTPS URL';
  }
  if (url.protocol !== 'https:') {
    return 'Only HTTPS URLs are accepted';
  }
  if (!url.hostname) {
    return 'The URL must include a host';
  }
  if (url.username || url.password) {
    return 'URLs containing embedded credentials are not accepted';
  }
  if (mode === 'individual') {
    return null;
  }
  const values = url.searchParams.getAll('roomDetails');
  if (values.length === 0) {
    return 'Battles URL must include roomDetails';
  }
  if (values.length !== 1) {
    return 'Battles URL must include roomDetails only once';
  }
  if (!values[0]) {
    return 'roomDetails cannot be empty';
  }
  try {
    const normalized = values[0].replace(/-/g, '+').replace(/_/g, '/');
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(normalized)) {
      throw new Error('Invalid Base64');
    }
    const decoded: unknown = JSON.parse(Base64.decode(normalized));
    if (!decoded || Array.isArray(decoded) || typeof decoded !== 'object') {
      return 'Decoded roomDetails must be a non-empty JSON object';
    }
    if (Object.keys(decoded as object).length === 0) {
      return 'Decoded roomDetails must be a non-empty JSON object';
    }
  } catch {
    return 'roomDetails must be valid Base64 JSON';
  }
  return null;
}

export function isAllowedNavigation(launchUrl: string, candidate: string): boolean {
  try {
    const launch = new URL(launchUrl);
    const next = new URL(candidate);
    if (next.protocol !== 'https:') {
      return false;
    }
    const host = next.hostname.toLowerCase();
    return (
      host === launch.hostname.toLowerCase() ||
      host === 'gamezop.com' ||
      host.endsWith('.gamezop.com') ||
      host === 'umogames.com' ||
      host.endsWith('.umogames.com')
    );
  } catch {
    return false;
  }
}
