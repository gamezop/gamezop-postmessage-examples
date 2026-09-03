import {eventReducer, initialEventState, parseGameEvent} from '../src/events';

describe('event parsing', () => {
  for (const state of ['loaded', 'start', 'playing', 'over', 'levelup']) {
    it(`parses individual ${state}`, () => {
      const event = parseGameEvent({state, score: 1, future: true}, 1);
      expect(event.family).toBe('individual');
      expect(event.status).toBe('valid');
      expect(event.fields.future).toBe(true);
    });
  }

  for (const eventName of ['match_found', 'match_not_found', 'match_start', 'match_playing', 'match_over', 'match_result', 'go_home']) {
    it(`parses Battles ${eventName}`, () => {
      const event = parseGameEvent(JSON.stringify({event: eventName}), 1);
      expect(event.family).toBe('battles');
      expect(event.status).toBe('valid');
    });
  }

  it('preserves unknown values and malformed input', () => {
    expect(parseGameEvent({state: 'future_state'}, 1).status).toBe('unknown');
    expect(parseGameEvent('{broken', 2).status).toBe('malformed');
  });
});

it('keeps the newest 500 entries and clears', () => {
  let state = initialEventState;
  for (let index = 0; index < 505; index += 1) {
    state = eventReducer(state, {type: 'capture', body: {state: 'playing', score: index}});
  }
  expect(state.events).toHaveLength(500);
  expect(state.events[0].fields.score).toBe(5);
  expect(state.dropped).toBe(5);
  state = eventReducer(state, {type: 'clear'});
  expect(state.events).toHaveLength(0);
});
