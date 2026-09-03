import React, {useEffect, useMemo, useRef, useState} from 'react';
import {BackHandler, Linking, PanResponder, Pressable, StyleSheet, Text, useWindowDimensions, View} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import WebView from 'react-native-webview';
import type {WebViewNavigation} from 'react-native-webview';
import type {EventAction, EventState, GameMode} from './events';
import {EventInspector} from './EventInspector';
import {isAllowedNavigation} from './urlPolicy';

export function GameScreen({mode, url, eventState, dispatch, onClose}: {
  mode: GameMode;
  url: string;
  eventState: EventState;
  dispatch: React.Dispatch<EventAction>;
  onClose: () => void;
}) {
  const insets = useSafeAreaInsets();
  const window = useWindowDimensions();
  const webView = useRef<WebView>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [canGoBack, setCanGoBack] = useState(false);
  const [logsOpen, setLogsOpen] = useState(false);
  const [pill, setPill] = useState({x: 12, y: 12});
  const dragOrigin = useRef(pill);
  const moved = useRef(false);
  const contentHeight = Math.max(0, window.height - insets.top - insets.bottom - 58);
  const maxX = Math.max(0, window.width - 100);
  const maxY = Math.max(0, contentHeight - 52);

  useEffect(() => {
    setPill(previous => ({x: Math.min(previous.x, maxX), y: Math.min(previous.y, maxY)}));
  }, [maxX, maxY]);

  const goBackOrClose = () => {
    if (canGoBack) webView.current?.goBack();
    else onClose();
  };

  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      goBackOrClose();
      return true;
    });
    return () => subscription.remove();
  });

  const panResponder = useMemo(() => PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dx) + Math.abs(gesture.dy) > 5,
    onPanResponderGrant: () => { dragOrigin.current = pill; moved.current = false; },
    onPanResponderMove: (_, gesture) => {
      moved.current = true;
      setPill({
        x: Math.max(0, Math.min(maxX, dragOrigin.current.x + gesture.dx)),
        y: Math.max(0, Math.min(maxY, dragOrigin.current.y + gesture.dy)),
      });
    },
  }), [maxX, maxY, pill]);

  const navigationAllowed = (candidate: string) => {
    if (isAllowedNavigation(url, candidate)) return true;
    if (candidate.startsWith('https://')) {
      Linking.openURL(candidate).catch(() => undefined);
    }
    return false;
  };

  return (
    <View style={[styles.screen, {paddingTop: insets.top, paddingBottom: insets.bottom}]}>
      <View style={styles.topBar}>
        <Pressable accessibilityLabel="Close game" hitSlop={10} onPress={goBackOrClose} style={styles.iconButton}>
          <Text style={styles.icon}>×</Text>
        </Pressable>
        <View style={styles.topCopy}>
          <Text style={styles.topTitle}>{mode === 'individual' ? 'Individual' : 'Battles'} game</Text>
          <Text style={styles.topStatus}>{loading ? 'Loading…' : 'ReactNativeWebView ready'}</Text>
        </View>
        <Pressable accessibilityLabel="Reload game" hitSlop={10} onPress={() => webView.current?.reload()} style={styles.iconButton}>
          <Text style={styles.reloadIcon}>↻</Text>
        </Pressable>
      </View>
      <View style={styles.gameArea}>
        <WebView
          allowsInlineMediaPlayback
          domStorageEnabled
          javaScriptEnabled
          mediaPlaybackRequiresUserAction={false}
          onError={event => { setLoading(false); setLoadError(event.nativeEvent.description); }}
          onLoadEnd={() => setLoading(false)}
          onLoadStart={() => { setLoading(true); setLoadError(null); }}
          onMessage={event => dispatch({type: 'capture', body: event.nativeEvent.data})}
          onNavigationStateChange={(state: WebViewNavigation) => setCanGoBack(state.canGoBack)}
          onShouldStartLoadWithRequest={request => navigationAllowed(request.url)}
          originWhitelist={['https://*']}
          ref={webView}
          setSupportMultipleWindows={false}
          sharedCookiesEnabled
          source={{uri: url}}
          style={styles.webView}
          thirdPartyCookiesEnabled
        />
        {logsOpen ? (
          <View style={[styles.inspectorPosition, {height: Math.min(300, Math.max(170, contentHeight * 0.44))}]}>
            <EventInspector onClear={() => dispatch({type: 'clear'})} onSelect={id => dispatch({type: 'select', id})} state={eventState} />
          </View>
        ) : null}
        <View {...panResponder.panHandlers} style={[styles.pillPosition, {left: pill.x, top: pill.y}]}>
          <Pressable accessibilityLabel="Toggle event logs" onPress={() => {
            if (!moved.current) setLogsOpen(value => !value);
            moved.current = false;
          }} style={styles.pill}>
            <Text style={styles.pillText}>{logsOpen ? 'Hide' : 'Logs'}</Text>
            <View style={styles.badge}><Text style={styles.badgeText}>{eventState.events.length}</Text></View>
          </Pressable>
        </View>
        {loadError ? (
          <View style={styles.errorCard}>
            <Text style={styles.errorTitle}>Could not load page</Text>
            <Text style={styles.errorMessage}>{loadError}</Text>
            <Pressable onPress={() => webView.current?.reload()} style={styles.retryButton}><Text style={styles.retryText}>Try again</Text></Pressable>
          </View>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {backgroundColor: '#ffffff', flex: 1},
  topBar: {alignItems: 'center', backgroundColor: '#ffffff', elevation: 4, flexDirection: 'row', height: 58, paddingHorizontal: 6, shadowColor: '#000', shadowOpacity: 0.12, shadowRadius: 4, zIndex: 5},
  iconButton: {alignItems: 'center', height: 44, justifyContent: 'center', width: 44},
  icon: {color: '#111827', fontSize: 34, fontWeight: '300', lineHeight: 37},
  reloadIcon: {color: '#111827', fontSize: 28, lineHeight: 32},
  topCopy: {flex: 1, paddingHorizontal: 6},
  topTitle: {color: '#111827', fontWeight: '700'},
  topStatus: {color: '#64748b', fontSize: 11, marginTop: 2},
  gameArea: {flex: 1}, webView: {flex: 1},
  inspectorPosition: {bottom: 8, left: 8, position: 'absolute', right: 8, zIndex: 10},
  pillPosition: {position: 'absolute', zIndex: 12},
  pill: {alignItems: 'center', backgroundColor: '#111a2e', borderRadius: 24, flexDirection: 'row', height: 48, justifyContent: 'center', paddingHorizontal: 12, width: 92},
  pillText: {color: '#67e8f9', fontWeight: '800'},
  badge: {alignItems: 'center', backgroundColor: '#0e7490', borderRadius: 10, justifyContent: 'center', marginLeft: 7, minHeight: 20, minWidth: 20, paddingHorizontal: 5},
  badgeText: {color: '#ffffff', fontSize: 10, fontWeight: '800'},
  errorCard: {alignSelf: 'center', backgroundColor: '#ffffff', borderRadius: 16, elevation: 8, maxWidth: 330, padding: 20, position: 'absolute', top: '34%', width: '82%'},
  errorTitle: {color: '#111827', fontSize: 18, fontWeight: '800'},
  errorMessage: {color: '#64748b', marginTop: 7},
  retryButton: {alignItems: 'center', backgroundColor: '#0f766e', borderRadius: 9, marginTop: 14, paddingVertical: 10},
  retryText: {color: '#ffffff', fontWeight: '800'},
});
