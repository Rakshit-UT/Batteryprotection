/*
  Wi-Fi bridge for the EV Battery Monitor app.
  Merge this into the existing ESP32 monitor sketch or upload as a standalone
  sketch after installing Adafruit INA219, Adafruit SSD1306, Adafruit GFX,
  DHT sensor library, and ArduinoJson 7.
*/
#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_INA219.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <DHT.h>
#include <math.h>

#define SDA_PIN 21
#define SCL_PIN 22
#define DHT_PIN 4
#define DHT_TYPE DHT11
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define OLED_ADDRESS 0x3C
#define INA_ADDRESS 0x40

// Set these to your 2.4 GHz Wi-Fi network credentials.
const char* WIFI_SSID = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
Adafruit_INA219 ina219(INA_ADDRESS);
DHT dht(DHT_PIN, DHT_TYPE);
WebServer server(80);

bool oledOK = false;
bool inaOK = false;
float voltage = NAN, current = NAN, temperature = NAN, humidity = NAN, soc = NAN;
unsigned long lastSample = 0;

float calculateSOC(float v) {
  const float volts[] = {11.80, 12.00, 12.20, 12.40, 12.60, 12.70};
  const float percent[] = {0, 20, 40, 60, 90, 100};
  if (v <= volts[0]) return 0;
  if (v >= volts[5]) return 100;
  for (int i = 0; i < 5; i++) {
    if (v <= volts[i + 1])
      return percent[i] + (v - volts[i]) * (percent[i + 1] - percent[i]) / (volts[i + 1] - volts[i]);
  }
  return NAN;
}

void sampleSensors() {
  if (inaOK) {
    float busV = ina219.getBusVoltage_V();
    float shuntMV = ina219.getShuntVoltage_mV();
    voltage = busV + shuntMV / 1000.0;
    current = ina219.getCurrent_mA();
    if (voltage < 0 || voltage > 26.0) voltage = NAN;
    soc = isnan(voltage) ? NAN : calculateSOC(voltage);
  } else {
    voltage = current = soc = NAN;
    if (ina219.begin()) inaOK = true;
  }
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  if (!isnan(t)) temperature = t;
  if (!isnan(h)) humidity = h;
  lastSample = millis();
}

void sendStatus() {
  JsonDocument doc;
  if (isnan(voltage)) doc["voltage"] = nullptr; else doc["voltage"] = voltage;
  if (isnan(current)) doc["current"] = nullptr; else doc["current"] = current;
  if (isnan(soc)) doc["soc"] = nullptr; else doc["soc"] = soc;
  if (isnan(temperature)) doc["temperature"] = nullptr; else doc["temperature"] = temperature;
  if (isnan(humidity)) doc["humidity"] = nullptr; else doc["humidity"] = humidity;
  doc["ina_ok"] = inaOK;
  doc["updated_ms"] = lastSample;
  String body;
  serializeJson(doc, body);
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Cache-Control", "no-store");
  server.send(200, "application/json", body);
}

void setup() {
  Serial.begin(115200);
  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000);
  dht.begin();
  oledOK = display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDRESS);
  inaOK = ina219.begin();

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("ESP32 app address: http://");
  Serial.println(WiFi.localIP());

  server.on("/", HTTP_GET, []() {
    server.send(200, "text/plain", "VoltGuard ESP32 battery monitor. GET /api/status");
  });
  server.on("/api/status", HTTP_GET, sendStatus);
  server.onNotFound([]() { server.send(404, "text/plain", "Not found"); });
  server.begin();
  sampleSensors();
}

void loop() {
  server.handleClient();
  if (millis() - lastSample >= 2000) sampleSensors();
}
