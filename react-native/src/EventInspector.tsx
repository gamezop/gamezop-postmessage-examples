import Clipboard from '@react-native-clipboard/clipboard';
import React from 'react';
import {Pressable, ScrollView, StyleSheet, Text, View} from 'react-native';
import type {CapturedEvent, EventState} from './events';

export function EventInspector({
  state,
  onClear,
  onSelect,
}: {
  state: EventState;
  onClear: () => void;
  onSelect: (id: number) => void;
}) {
  const selected =
    state.events.find(event => event.id === state.selectedId) ??
    state.events[state.events.length - 1];
  return (
    <View style={styles.panel} testID="event-log-panel">
      <View style={styles.header}>
        <View style={styles.onlineDot} />
        <View style={styles.headerCopy}>
          <Text style={styles.headerTitle}>Event logs</Text>
          <Text style={styles.headerSubtitle}>
            {state.events.length} captured
            {state.dropped ? ` · ${state.dropped} discarded` : ''}
          </Text>
        </View>
        <Pressable disabled={!state.events.length} onPress={onClear} style={styles.clearButton}>
          <Text style={styles.clearText}>Clear</Text>
        </Pressable>
      </View>
      {!state.events.length ? (
        <View style={styles.empty}>
          <Text style={styles.emptyText}>Waiting for ReactNativeWebView.postMessage events</Text>
        </View>
      ) : (
        <View style={styles.body}>
          <ScrollView style={styles.timeline}>
            {[...state.events].reverse().map(event => (
              <EventTile
                event={event}
                key={event.id}
                onPress={() => onSelect(event.id)}
                selected={event.id === selected?.id}
              />
            ))}
          </ScrollView>
          {selected ? <EventDetails event={selected} /> : null}
        </View>
      )}
    </View>
  );
}

function EventTile({event, selected, onPress}: {event: CapturedEvent; selected: boolean; onPress: () => void}) {
  const color = eventColor(event);
  return (
    <Pressable onPress={onPress} style={[styles.tile, selected ? eventBorderStyle(event) : styles.tileUnselected]}>
      <Text numberOfLines={1} style={[styles.tileName, {color}]}>{event.name}</Text>
      <Text style={styles.tileFamily}>{event.family}</Text>
    </Pressable>
  );
}

function EventDetails({event}: {event: CapturedEvent}) {
  return (
    <ScrollView contentContainerStyle={styles.details} style={styles.detailsScroll}>
      <View style={styles.detailsHeader}>
        <View style={styles.headerCopy}>
          <Text style={styles.detailsTitle}>{event.name}</Text>
          <Text style={{color: eventColor(event)}}>{event.status}</Text>
        </View>
        <Pressable onPress={() => Clipboard.setString(event.rawJson)} style={styles.copyButton}>
          <Text style={styles.copyText}>Copy JSON</Text>
        </Pressable>
      </View>
      {Object.keys(event.fields).sort().map(key => (
        <View key={key} style={styles.field}>
          <Text style={styles.fieldKey}>{key}</Text>
          <Text selectable style={styles.fieldValue}>
            {typeof event.fields[key] === 'object'
              ? JSON.stringify(event.fields[key], null, 2)
              : String(event.fields[key])}
          </Text>
        </View>
      ))}
      <Text style={styles.rawTitle}>Raw JSON</Text>
      <Text selectable style={styles.raw}>{event.prettyJson}</Text>
    </ScrollView>
  );
}

function eventColor(event: CapturedEvent) {
  if (event.status === 'malformed') return '#dc2626';
  if (event.status === 'unknown') return '#d97706';
  return event.family === 'battles' ? '#9333ea' : '#0f766e';
}

function eventBorderStyle(event: CapturedEvent) {
  if (event.status === 'malformed') return styles.tileMalformed;
  if (event.status === 'unknown') return styles.tileUnknown;
  return event.family === 'battles' ? styles.tileBattles : styles.tileIndividual;
}

const styles = StyleSheet.create({
  panel: {backgroundColor: '#ffffff', borderRadius: 18, elevation: 14, flex: 1, overflow: 'hidden', shadowColor: '#000', shadowOpacity: 0.24, shadowRadius: 18},
  header: {alignItems: 'center', borderBottomColor: '#e2e8f0', borderBottomWidth: 1, flexDirection: 'row', minHeight: 58, paddingHorizontal: 14},
  onlineDot: {backgroundColor: '#22c55e', borderRadius: 5, height: 10, marginRight: 10, width: 10},
  headerCopy: {flex: 1},
  headerTitle: {color: '#111827', fontWeight: '800'},
  headerSubtitle: {color: '#64748b', fontSize: 11, marginTop: 2},
  clearButton: {borderColor: '#cbd5e1', borderRadius: 9, borderWidth: 1, paddingHorizontal: 12, paddingVertical: 7},
  clearText: {color: '#334155', fontWeight: '700'},
  empty: {alignItems: 'center', flex: 1, justifyContent: 'center', padding: 18},
  emptyText: {color: '#64748b', textAlign: 'center'},
  body: {flex: 1, flexDirection: 'row'},
  timeline: {borderRightColor: '#e2e8f0', borderRightWidth: 1, maxWidth: 145, padding: 7, width: 145},
  tile: {backgroundColor: '#f8fafc', borderRadius: 9, borderWidth: 2, marginBottom: 7, padding: 8},
  tileUnselected: {borderColor: 'transparent'},
  tileMalformed: {borderColor: '#dc2626'},
  tileUnknown: {borderColor: '#d97706'},
  tileBattles: {borderColor: '#9333ea'},
  tileIndividual: {borderColor: '#0f766e'},
  tileName: {fontSize: 12, fontWeight: '800'},
  tileFamily: {color: '#64748b', fontSize: 10, marginTop: 2},
  detailsScroll: {flex: 1},
  details: {padding: 11},
  detailsHeader: {alignItems: 'center', flexDirection: 'row'},
  detailsTitle: {color: '#111827', fontSize: 17, fontWeight: '800'},
  copyButton: {borderColor: '#cbd5e1', borderRadius: 8, borderWidth: 1, paddingHorizontal: 9, paddingVertical: 6},
  copyText: {color: '#334155', fontSize: 11, fontWeight: '700'},
  field: {marginTop: 9},
  fieldKey: {color: '#334155', fontSize: 10, fontWeight: '800'},
  fieldValue: {color: '#475569', fontFamily: 'monospace', fontSize: 11, marginTop: 2},
  rawTitle: {color: '#334155', fontSize: 11, fontWeight: '800', marginTop: 13},
  raw: {backgroundColor: '#f1f5f9', borderRadius: 8, color: '#334155', fontFamily: 'monospace', fontSize: 10, marginTop: 5, padding: 9},
});
