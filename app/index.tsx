import { StyleSheet, Text, View, Button, NativeModules, PermissionsAndroid, Linking, Platform } from 'react-native';
import React, { useEffect } from 'react';

const { PerformanceMonitorModule, OverlayManager } = NativeModules;

export default function Index() {

  const requestOverlayPermission = async () => {
    if (Platform.OS === 'android') {
      if (Platform.Version >= 23) {
        try {
          const granted = await OverlayManager.requestOverlayPermission();
          if (granted) {
            console.log("Overlay permission granted");
          } else {
            console.log("Overlay permission denied");
            // Optionally, direct user to settings
            // Linking.openSettings();
          }
        } catch (e) {
          console.error(e);
        }
      } else {
        // For Android < 6.0, permission is granted at install time if in manifest
        console.log("Overlay permission should be granted at install time for Android < 6.0");
      }
    }
  };

  const requestUsageStatsPermission = async () => {
    if (Platform.OS === 'android') {
       try {
        const granted = await PerformanceMonitorModule.requestUsageStatsPermission();
        if (granted) {
          console.log("Usage Stats permission seems granted or already was.");
        } else {
          console.log("Usage Stats permission denied or not available through this method. Please grant manually via Settings.");
          // Direct user to settings for PACKAGE_USAGE_STATS
          Linking.openSettings(); // This opens general app settings, user needs to find "Usage access"
        }
       } catch (e) {
        console.error("Error requesting Usage Stats permission", e);
        Linking.openSettings();
       }
    }
  };

  useEffect(() => {
    // Request permissions on component mount or specific user action
    // For PoC, let's have buttons do it.
  }, []);

  const toggleOverlay = () => {
    if (OverlayManager) {
      OverlayManager.toggleOverlay();
    } else {
      console.warn("OverlayManager native module not available.");
    }
  };

  const startPerformanceService = () => {
    if (PerformanceMonitorModule) {
      PerformanceMonitorModule.startService();
    } else {
      console.warn("PerformanceMonitorModule native module not available.");
    }
  };

  const stopPerformanceService = () => {
    if (PerformanceMonitorModule) {
      PerformanceMonitorModule.stopService();
    } else {
      console.warn("PerformanceMonitorModule native module not available.");
    }
  };


  return (
    <View style={styles.container}>
      <Text style={styles.title}>Performance Monitor PoC</Text>
      <Button title="Request Overlay Permission" onPress={requestOverlayPermission} />
      <Button title="Toggle Simple Overlay" onPress={toggleOverlay} />
      <View style={styles.separator} />
      <Button title="Request Usage Stats Permission" onPress={requestUsageStatsPermission} />
      <Button title="Start Monitoring Service" onPress={startPerformanceService} />
      <Button title="Stop Monitoring Service" onPress={stopPerformanceService} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  separator: {
    marginVertical: 15,
    height: 1,
    width: '80%',
    backgroundColor: '#eee',
  },
});
