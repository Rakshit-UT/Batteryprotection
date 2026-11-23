# VoltGuard Battery Monitor

A minimal Android companion for the ESP32 + INA219 + DHT11 battery monitor. No account or cloud service is used. The app polls a JSON endpoint on the same Wi-Fi network every two seconds and shows under-voltage, over-voltage, sensor, and connection states.

## Battery thresholds

The starter configuration treats the measured battery as a **12 V lead-acid battery**: below 11.8 V is under-voltage and above 14.7 V is over-voltage. These are configurable in the app. Confirm safe limits for your actual battery chemistry and charging system before relying on alerts. The sketch's voltage-based SOC curve is only for a resting lead-acid battery; it is not suitable for lithium packs.

## ESP32 connection

1. Copy `esp32/BatteryMonitorWiFi.ino` into your Arduino sketch folder (or merge its Wi-Fi server and `/api/status` handler into your existing sketch).
2. Set `WIFI_SSID` and `WIFI_PASSWORD`, install ArduinoJson 7, and upload it to the ESP32.
3. Connect the phone and ESP32 to the same Wi-Fi network. Open the app, enter the ESP32's printed IP address, and tap **Connect**.

The JSON endpoint returns voltage in V, current in mA, SOC in percent, temperature in °C, humidity in percent, and an `ina_ok` boolean. This keeps the app independent of third-party services.

## Build the APK

The GitHub Actions workflow builds a debug APK on every push and when run manually. Open the repository's **Actions** tab, select **Build Android APK**, and open the newest successful run. Download the `VoltGuard-debug-apk` artifact from that run. The artifact is a debug-signed APK suitable for direct installation; it is not a Play Store release.

For a local build, open this directory in Android Studio and let Gradle sync. Run the `app` configuration on a phone, or select **Build > Build APK(s)**. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install a built APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
