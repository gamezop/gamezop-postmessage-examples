import {Base64} from 'js-base64';
import {isAllowedNavigation, validateLaunchUrl} from '../src/urlPolicy';

function battlesUrl(details: object) {
  const encoded = Base64.encode(JSON.stringify(details), true).replaceAll('=', '');
  return `https://11353.play.gamezop.com/g/example?roomDetails=${encoded}`;
}

describe('launch URL policy', () => {
  it('accepts HTTPS individual and valid Base64 Battles URLs', () => {
    expect(validateLaunchUrl('https://11353.play.gamezop.com/g/example', 'individual')).toBeNull();
    expect(validateLaunchUrl(battlesUrl({roomId: 'ABC01'}), 'battles')).toBeNull();
  });

  it('rejects credentials, missing, duplicate, and malformed roomDetails', () => {
    expect(validateLaunchUrl('https://user@example.com', 'individual')).not.toBeNull();
    expect(validateLaunchUrl('https://example.com', 'battles')).not.toBeNull();
    expect(validateLaunchUrl('https://example.com?roomDetails=bad', 'battles')).not.toBeNull();
    expect(validateLaunchUrl(`${battlesUrl({x: 1})}&roomDetails=again`, 'battles')).not.toBeNull();
  });

  it('allows the launch host and documented Gamezop hosts', () => {
    const launch = 'https://partner.example/game';
    expect(isAllowedNavigation(launch, 'https://partner.example/next')).toBe(true);
    expect(isAllowedNavigation(launch, 'https://11353.play.gamezop.com/g/id')).toBe(true);
    expect(isAllowedNavigation(launch, 'https://untrusted.example')).toBe(false);
  });
});
