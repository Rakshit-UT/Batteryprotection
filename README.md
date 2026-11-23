# VoltGuard Battery Monitor

Minimal Android companion app for the ESP32 + INA219 + DHT11 battery monitor. No account or cloud service is used. The app polls the ESP32 over the same Wi-Fi network and shows battery voltage, under/over-voltage alerts, SOC, current, temperature, humidity, and sensor connection state.

## Download the APK

GitHub Actions builds a debug APK on every push. Open **Actions → Build Android APK**, select the latest successful run, and download the `VoltGuard-debug-apk` artifact. It can be installed directly on an Android phone; it is debug signed and not a Play Store release.

## Build and flash

1. Open the `battery-monitor` folder in Android Studio to build locally.
2. Follow `battery-monitor/README.md` to upload the ESP32 Wi-Fi endpoint.
3. Connect the phone to the same Wi-Fi, open VoltGuard, and enter the ESP32 IP printed in Serial Monitor.

The threshold defaults (11.8 V low, 14.7 V high) are a configurable 12 V lead-acid preset. Confirm safe thresholds for your battery chemistry and charging system.