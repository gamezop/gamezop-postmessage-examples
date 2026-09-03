import React, {useReducer} from 'react';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import ReactTestRenderer from 'react-test-renderer';
import {eventReducer, initialEventState} from '../src/events';
import {GameScreen} from '../src/GameScreen';

jest.mock('react-native-webview', () => {
  const ReactModule = require('react');
  const {View} = require('react-native');
  return {
    __esModule: true,
    default: ReactModule.forwardRef((props: object, ref: React.Ref<unknown>) =>
      ReactModule.createElement(View, {...props, ref, testID: 'mock-webview'}),
    ),
  };
});

jest.mock('@react-native-clipboard/clipboard', () => ({
  __esModule: true,
  default: {setString: jest.fn()},
}));

function Harness() {
  const [state, dispatch] = useReducer(eventReducer, initialEventState);
  return (
    <SafeAreaProvider
      initialMetrics={{
        frame: {x: 0, y: 0, width: 390, height: 844},
        insets: {top: 47, left: 0, right: 0, bottom: 34},
      }}>
      <GameScreen
        dispatch={dispatch}
        eventState={state}
        mode="individual"
        onClose={jest.fn()}
        url="https://11353.play.gamezop.com/g/example"
      />
    </SafeAreaProvider>
  );
}

test('WebView onMessage reaches the draggable inspector', async () => {
  let renderer: ReactTestRenderer.ReactTestRenderer;
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<Harness />);
  });
  const webView = renderer!.root.findByProps({testID: 'mock-webview'});
  await ReactTestRenderer.act(() => {
    webView.props.onMessage({
      nativeEvent: {data: JSON.stringify({state: 'loaded', score: 0})},
    });
  });
  const toggle = renderer!.root.findByProps({accessibilityLabel: 'Toggle event logs'});
  await ReactTestRenderer.act(() => toggle.props.onPress());
  expect(renderer!.root.findByProps({testID: 'event-log-panel'})).toBeTruthy();
  expect(renderer!.root.findAllByProps({children: 'loaded'}).length).toBeGreaterThan(0);
});
