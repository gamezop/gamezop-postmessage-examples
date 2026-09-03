import React, {useReducer, useState} from 'react';
import {StatusBar} from 'react-native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {eventReducer, initialEventState, type GameMode} from './src/events';
import {GameScreen} from './src/GameScreen';
import {LauncherScreen} from './src/LauncherScreen';

interface Launch {
  mode: GameMode;
  url: string;
}

export default function App() {
  const [launch, setLaunch] = useState<Launch | null>(null);
  const [events, dispatch] = useReducer(eventReducer, initialEventState);
  return (
    <SafeAreaProvider>
      <StatusBar barStyle="dark-content" />
      {launch ? (
        <GameScreen
          dispatch={dispatch}
          eventState={events}
          mode={launch.mode}
          onClose={() => setLaunch(null)}
          url={launch.url}
        />
      ) : (
        <LauncherScreen onLaunch={(mode, url) => setLaunch({mode, url})} />
      )}
    </SafeAreaProvider>
  );
}
