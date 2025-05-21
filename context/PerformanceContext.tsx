import React, { createContext, useContext, useState, useEffect } from 'react';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { useColorScheme } from 'react-native';

interface PerformanceState {
  metrics: {
    fps: number;
    cpu: number;
    gpu: number;
    temp: number;
    ram: number;
  };
  settings: {
    showFPS: boolean;
    showCPU: boolean;
    showGPU: boolean;
    showTemp: boolean;
    showRAM: boolean;
    transparency: number;
    isDarkMode: boolean;
    isLandscape: boolean;
    isPinned: boolean;
    isVisible: boolean;
    autoMinimize: boolean;
  };
  isGameMode: boolean;
  updateSettings: (key: string, value: any) => void;
  toggleGameMode: () => void;
}

const PerformanceContext = createContext<PerformanceState | undefined>(undefined);

export const PerformanceProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const colorScheme = useColorScheme();
  const [metrics, setMetrics] = useState({
    fps: 60,
    cpu: 30,
    gpu: 25,
    temp: 45,
    ram: 60,
  });

  const [settings, setSettings] = useState({
    showFPS: true,
    showCPU: true,
    showGPU: true,
    showTemp: true,
    showRAM: true,
    transparency: 0.8,
    isDarkMode: colorScheme === 'dark',
    isLandscape: false,
    isPinned: false,
    isVisible: true,
    autoMinimize: false,
  });

  const [isGameMode, setIsGameMode] = useState(false);

  useEffect(() => {
    setupNotifications();
    startMetricsMonitoring();
  }, []);

  const setupNotifications = async () => {
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('performance', {
        name: 'Performance Monitor',
        importance: Notifications.AndroidImportance.LOW,
        vibrationPattern: [0, 0, 0, 0],
        lightColor: '#FF231F7C',
      });
    }

    await Notifications.requestPermissionsAsync();
  };

  const startMetricsMonitoring = () => {
    // Simulated metrics updates - Replace with actual performance monitoring
    const interval = setInterval(() => {
      setMetrics(prev => ({
        fps: Math.floor(55 + Math.random() * 10),
        cpu: Math.floor(20 + Math.random() * 20),
        gpu: Math.floor(15 + Math.random() * 20),
        temp: Math.floor(40 + Math.random() * 10),
        ram: Math.floor(50 + Math.random() * 20),
      }));
    }, 1000);

    return () => clearInterval(interval);
  };

  const updateSettings = (key: string, value: any) => {
    setSettings(prev => ({
      ...prev,
      [key]: value,
    }));
  };

  const toggleGameMode = () => {
    setIsGameMode(prev => !prev);
    if (!isGameMode) {
      showGameModeNotification();
    }
  };

  const showGameModeNotification = async () => {
    await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Performance Monitor Active',
        body: 'Monitoring game performance...',
        data: { type: 'gameMode' },
      },
      trigger: null,
    });
  };

  return (
    <PerformanceContext.Provider
      value={{
        metrics,
        settings,
        isGameMode,
        updateSettings,
        toggleGameMode,
      }}
    >
      {children}
    </PerformanceContext.Provider>
  );
};

export const usePerformance = () => {
  const context = useContext(PerformanceContext);
  if (context === undefined) {
    throw new Error('usePerformance must be used within a PerformanceProvider');
  }
  return context;
};
