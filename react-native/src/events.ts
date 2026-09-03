export type GameMode = 'individual' | 'battles';
export type EventFamily = 'individual' | 'battles' | 'unknown';
export type ValidationStatus = 'valid' | 'unknown' | 'malformed';

export interface CapturedEvent {
  id: number;
  receivedAt: string;
  rawJson: string;
  prettyJson: string;
  family: EventFamily;
  name: string;
  fields: Record<string, unknown>;
  status: ValidationStatus;
}

export const INDIVIDUAL_STATES = new Set([
  'loaded',
  'start',
  'playing',
  'over',
  'levelup',
]);
export const BATTLES_EVENTS = new Set([
  'match_found',
  'match_not_found',
  'match_start',
  'match_playing',
  'match_over',
  'match_result',
  'go_home',
]);

export function parseGameEvent(
  body: string | object,
  id: number,
  receivedAt = new Date(),
): CapturedEvent {
  let rawJson = typeof body === 'string' ? body : JSON.stringify(body);
  try {
    const decoded: unknown = typeof body === 'string' ? JSON.parse(body) : body;
    if (!decoded || Array.isArray(decoded) || typeof decoded !== 'object') {
      throw new Error('Payload is not a JSON object');
    }
    const fields = decoded as Record<string, unknown>;
    const state = fields.state;
    const event = fields.event;
    let family: EventFamily = 'unknown';
    let name = 'unknown_payload';
    let status: ValidationStatus = 'unknown';
    if (typeof state === 'string') {
      family = 'individual';
      name = state;
      status = INDIVIDUAL_STATES.has(state) ? 'valid' : 'unknown';
    } else if (typeof event === 'string') {
      family = 'battles';
      name = event;
      status = BATTLES_EVENTS.has(event) ? 'valid' : 'unknown';
    }
    return {
      id,
      receivedAt: receivedAt.toISOString(),
      rawJson,
      prettyJson: JSON.stringify(fields, null, 2),
      family,
      name,
      fields,
      status,
    };
  } catch (error) {
    if (typeof rawJson !== 'string') {
      rawJson = String(body);
    }
    return {
      id,
      receivedAt: receivedAt.toISOString(),
      rawJson,
      prettyJson: rawJson,
      family: 'unknown',
      name: 'malformed_json',
      fields: {error: error instanceof Error ? error.message : String(error)},
      status: 'malformed',
    };
  }
}

export interface EventState {
  events: CapturedEvent[];
  nextId: number;
  selectedId?: number;
  dropped: number;
}

export type EventAction =
  | {type: 'capture'; body: string | object; now?: Date}
  | {type: 'select'; id: number}
  | {type: 'clear'};

export const initialEventState: EventState = {
  events: [],
  nextId: 1,
  dropped: 0,
};

export function eventReducer(state: EventState, action: EventAction): EventState {
  if (action.type === 'clear') {
    return {...initialEventState, nextId: state.nextId};
  }
  if (action.type === 'select') {
    return {...state, selectedId: action.id};
  }
  const event = parseGameEvent(action.body, state.nextId, action.now);
  const all = [...state.events, event];
  const overflow = Math.max(0, all.length - 500);
  return {
    events: overflow ? all.slice(overflow) : all,
    nextId: state.nextId + 1,
    selectedId: event.id,
    dropped: state.dropped + overflow,
  };
}
