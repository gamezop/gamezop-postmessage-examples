import React, {useState} from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {GameMode} from './events';
import {validateLaunchUrl} from './urlPolicy';

export function LauncherScreen({
  onLaunch,
}: {
  onLaunch: (mode: GameMode, url: string) => void;
}) {
  const insets = useSafeAreaInsets();
  const [mode, setMode] = useState<GameMode>('individual');
  const [urls, setUrls] = useState<Record<GameMode, string>>({
    individual: '',
    battles: '',
  });
  const [error, setError] = useState<string | null>(null);

  const launch = () => {
    const validation = validateLaunchUrl(urls[mode], mode);
    setError(validation);
    if (!validation) {
      onLaunch(mode, urls[mode].trim());
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <ScrollView
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={[
          styles.container,
          {paddingTop: insets.top + 28, paddingBottom: insets.bottom + 28},
        ]}>
        <View style={styles.content}>
          <Text style={styles.title}>Gamezop Event Bridge</Text>
          <Text style={styles.subtitle}>
            Launch a real Gamezop URL and inspect bridge events in real time.
          </Text>
          <View accessibilityRole="tablist" style={styles.tabs}>
            {(['individual', 'battles'] as const).map(value => (
              <Pressable
                accessibilityRole="tab"
                accessibilityState={{selected: value === mode}}
                key={value}
                onPress={() => {
                  setMode(value);
                  setError(null);
                }}
                style={[styles.tab, value === mode && styles.activeTab]}>
                <Text style={value === mode ? styles.activeTabText : styles.tabText}>
                  {value === 'individual' ? 'Individual' : 'Battles'}
                </Text>
              </Pressable>
            ))}
          </View>
          <View style={styles.card}>
            <Text style={styles.cardTitle}>
              {mode === 'individual' ? 'Individual game URL' : 'Final Battles URL'}
            </Text>
            <Text style={styles.help}>
              {mode === 'individual'
                ? 'Paste a Gamezop game URL or Unique Link.'
                : 'Paste the generated Battles URL containing roomDetails.'}
            </Text>
            <TextInput
              accessibilityLabel="Game URL"
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              onChangeText={text => {
                setUrls(previous => ({...previous, [mode]: text}));
                setError(null);
              }}
              onSubmitEditing={launch}
              placeholder="https://…"
              placeholderTextColor="#718096"
              returnKeyType="go"
              style={[styles.input, error && styles.inputError]}
              value={urls[mode]}
            />
            {error ? <Text style={styles.error}>{error}</Text> : null}
            <Pressable accessibilityRole="button" onPress={launch} style={styles.launchButton}>
              <Text style={styles.launchText}>
                Launch {mode === 'individual' ? 'Individual' : 'Battles'}
              </Text>
            </Pressable>
          </View>
          <Text style={styles.footnote}>
            Events remain in memory and are cleared when the app process ends.
          </Text>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {flex: 1, backgroundColor: '#f4f7fb'},
  container: {flexGrow: 1, justifyContent: 'center', paddingHorizontal: 22},
  content: {alignSelf: 'center', maxWidth: 680, width: '100%'},
  title: {color: '#111827', fontSize: 34, fontWeight: '800'},
  subtitle: {color: '#4b5563', fontSize: 17, lineHeight: 24, marginTop: 8},
  tabs: {backgroundColor: '#e2e8f0', borderRadius: 14, flexDirection: 'row', marginTop: 28, padding: 4},
  tab: {alignItems: 'center', borderRadius: 11, flex: 1, paddingVertical: 11},
  activeTab: {backgroundColor: '#ffffff'},
  tabText: {color: '#475569', fontWeight: '600'},
  activeTabText: {color: '#0f172a', fontWeight: '800'},
  card: {backgroundColor: '#ffffff', borderRadius: 20, elevation: 3, marginTop: 18, padding: 20, shadowColor: '#0f172a', shadowOpacity: 0.08, shadowRadius: 16},
  cardTitle: {color: '#111827', fontSize: 19, fontWeight: '800'},
  help: {color: '#64748b', lineHeight: 20, marginTop: 6},
  input: {borderColor: '#cbd5e1', borderRadius: 12, borderWidth: 1, color: '#111827', fontSize: 16, marginTop: 16, paddingHorizontal: 14, paddingVertical: 13},
  inputError: {borderColor: '#dc2626'},
  error: {color: '#b91c1c', marginTop: 7},
  launchButton: {alignItems: 'center', backgroundColor: '#0f766e', borderRadius: 12, marginTop: 14, paddingVertical: 14},
  launchText: {color: '#ffffff', fontSize: 16, fontWeight: '800'},
  footnote: {color: '#64748b', fontSize: 12, marginTop: 18},
});
