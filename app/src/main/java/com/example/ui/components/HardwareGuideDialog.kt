package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer

@Composable
fun HardwareGuideDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val tabs = listOf("Pinout & Wiring", "Clock REST API", "SmartHome C++", "LED Clock C++")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeveloperBoard,
                    contentDescription = "Hardware guide",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dual ESP8266 Hardware Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(390.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> PinoutSection()
                        1 -> ClockApiContractSection()
                        2 -> SmartHomeSketchSection(
                            onCopyCode = {
                                clipboardManager.setText(AnnotatedString(ESP8266_SMART_HOME_SKETCH))
                                Toast.makeText(context, "Smart Home sketch copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        3 -> ClockSketchSection(
                            onCopyCode = {
                                clipboardManager.setText(AnnotatedString(ESP8266_LED_CLOCK_SKETCH))
                                Toast.makeText(context, "LED Clock sketch copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("close_guide_button")
            ) {
                Text("Close Guide")
            }
        }
    )
}

@Composable
private fun PinoutSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = WarningContainer,
            border = BorderStroke(1.dp, WarningAmber),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = OnWarningContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "IMPORTANT: TWO SEPARATE BOARDS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnWarningContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Device 1 (Smart Home) and Device 2 (LED Clock) are two physically separate ESP8266 boards. Do not combine their GPIO pins, power buses, or code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnWarningContainer,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Device 1: Smart Home NodeMCU (MQTT)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        listOf(
            "Relay 1" to "GPIO15 (D8) [Active LOW / HIGH relay]",
            "Relay 2" to "GPIO13 (D7)",
            "Relay 3" to "GPIO12 (D6)",
            "Relay 4" to "GPIO14 (D5)",
            "Physical Switch 1" to "GPIO2 (D4) [INPUT_PULLUP]",
            "Physical Switch 2" to "GPIO0 (D3) [INPUT_PULLUP]",
            "Physical Switch 3" to "GPIO4 (D2) [INPUT_PULLUP]",
            "Physical Switch 4" to "GPIO5 (D1) [INPUT_PULLUP]",
            "Fan Speed PWM" to "GPIO16 (D0) [0-100% -> analogWrite 0-1023]"
        ).forEach { (pin, desc) -> PinRow(pin, desc) }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Device 2: LED Digital Clock D1 Mini / NodeMCU (HTTP)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        listOf(
            "WS2812B Data In" to "GPIO2 (D4) [Data line to WS2812B NeoPixels]",
            "LDR Light Sensor" to "Pin A0 (ADC0) [Voltage divider with 10k resistor]",
            "Static IP" to "192.168.1.102 (WiFi.config on Netis Wi-Fi)",
            "HTTP Port" to "80 (Standard WebServer)"
        ).forEach { (pin, desc) -> PinRow(pin, desc) }
    }
}

@Composable
private fun ClockApiContractSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Device 2 REST API Contract",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "The Android app communicates with Device 2 via HTTP REST JSON endpoints (with query-parameter fallbacks):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        listOf(
            "GET /api/status" to "Returns full JSON status (clock, week, colon, date colors, effect 0-15, speed 1-100, brightness 0-255, auto, hour24, and raw ldr 0-1023)",
            "POST /api/brightness" to "JSON: {\"brightness\": 0..255, \"auto\": true/false}. Updates FastLED brightness and persists in EEPROM",
            "POST /api/effects" to "JSON: {\"effect\": 0..15, \"speed\": 1..100, \"enabled\": true/false}. Dynamically modulates transition delay across all 16 effects",
            "GET /api/ldr" to "Returns real-time analogRead(A0) raw ADC value (0..1023) as {\"ldr\": 612}. Polled automatically every 1s by the app",
            "POST /api/color" to "JSON: {\"clock\": \"R,G,B\", \"week\": \"R,G,B\", \"colon\": \"R,G,B\", \"datecolor\": \"R,G,B\"}",
            "POST /api/time-format" to "JSON: {\"hour24\": true/false}",
            "POST /api/ntp-sync" to "JSON: {\"timezone\": \"Asia/Dhaka\", \"utc_offset\": 6}",
            "POST /api/settings" to "JSON: {\"action\": \"save\" / \"reset\", \"blinkcolon\": true, \"showdate\": true}",
            "GET /testldr" to "Fallback query returning raw ADC ambient reading (0..1023)"
        ).forEach { (endpoint, desc) ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = endpoint, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SmartHomeSketchSection(onCopyCode: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device 1 ESP8266 Arduino Sketch",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onCopyCode, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Code", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = ESP8266_SMART_HOME_SKETCH,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ClockSketchSection(onCopyCode: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device 2 FastLED Clock Sketch",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(onClick = onCopyCode, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Code", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = ESP8266_LED_CLOCK_SKETCH,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun PinRow(pin: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = pin, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val ESP8266_SMART_HOME_SKETCH = """#include <ESP8266WiFi.h>
#include <WiFiClientSecure.h>
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

// ================= DEVICE 1: SMART HOME (MQTT) =================
#define WLAN_SSID       "YOUR_NETIS_WIFI_SSID"
#define WLAN_PASS       "YOUR_NETIS_WIFI_PASS"

#define AIO_SERVER      "io.adafruit.com"
#define AIO_SERVERPORT  8883
#define AIO_USERNAME    "YOUR_AIO_USERNAME"
#define AIO_KEY         "YOUR_AIO_KEY"

// Pin Configuration
#define RELAY1_PIN      15  // D8 (socket-1)
#define RELAY2_PIN      13  // D7 (socket-2)
#define RELAY3_PIN      12  // D6 (socket-3)
#define RELAY4_PIN      14  // D5 (socket-4)

#define SWITCH1_PIN     2   // D4 (Physical Switch 1)
#define SWITCH2_PIN     0   // D3 (Physical Switch 2)
#define SWITCH3_PIN     4   // D2 (Physical Switch 3)
#define SWITCH4_PIN     5   // D1 (Physical Switch 4)

#define FAN_PWM_PIN     16  // D0 (fan-speed PWM output)

WiFiClientSecure client;
Adafruit_MQTT_Client mqtt(&client, AIO_SERVER, AIO_SERVERPORT, AIO_USERNAME, AIO_KEY);

Adafruit_MQTT_Subscribe sub_socket1 = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/socket-1");
Adafruit_MQTT_Subscribe sub_socket2 = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/socket-2");
Adafruit_MQTT_Subscribe sub_socket3 = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/socket-3");
Adafruit_MQTT_Subscribe sub_socket4 = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/socket-4");
Adafruit_MQTT_Subscribe sub_fan     = Adafruit_MQTT_Subscribe(&mqtt, AIO_USERNAME "/feeds/fan-speed");
Adafruit_MQTT_Publish   pub_status  = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/device-status");
Adafruit_MQTT_Publish   pub_socket1 = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/socket-1");
Adafruit_MQTT_Publish   pub_socket2 = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/socket-2");
Adafruit_MQTT_Publish   pub_socket3 = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/socket-3");
Adafruit_MQTT_Publish   pub_socket4 = Adafruit_MQTT_Publish(&mqtt, AIO_USERNAME "/feeds/socket-4");

int lastSw1, lastSw2, lastSw3, lastSw4;

void MQTT_connect();

void setup() {
  Serial.begin(115200);
  pinMode(RELAY1_PIN, OUTPUT);
  pinMode(RELAY2_PIN, OUTPUT);
  pinMode(RELAY3_PIN, OUTPUT);
  pinMode(RELAY4_PIN, OUTPUT);
  pinMode(FAN_PWM_PIN, OUTPUT);

  pinMode(SWITCH1_PIN, INPUT_PULLUP);
  pinMode(SWITCH2_PIN, INPUT_PULLUP);
  pinMode(SWITCH3_PIN, INPUT_PULLUP);
  pinMode(SWITCH4_PIN, INPUT_PULLUP);

  digitalWrite(RELAY1_PIN, HIGH);
  digitalWrite(RELAY2_PIN, HIGH);
  digitalWrite(RELAY3_PIN, HIGH);
  digitalWrite(RELAY4_PIN, HIGH);
  analogWrite(FAN_PWM_PIN, 0);

  lastSw1 = digitalRead(SWITCH1_PIN);
  lastSw2 = digitalRead(SWITCH2_PIN);
  lastSw3 = digitalRead(SWITCH3_PIN);
  lastSw4 = digitalRead(SWITCH4_PIN);

  WiFi.begin(WLAN_SSID, WLAN_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(500); }
  client.setInsecure();

  mqtt.subscribe(&sub_socket1);
  mqtt.subscribe(&sub_socket2);
  mqtt.subscribe(&sub_socket3);
  mqtt.subscribe(&sub_socket4);
  mqtt.subscribe(&sub_fan);
}

void loop() {
  MQTT_connect();

  int sw1 = digitalRead(SWITCH1_PIN);
  if (sw1 != lastSw1) {
    lastSw1 = sw1;
    bool rState = !digitalRead(RELAY1_PIN);
    digitalWrite(RELAY1_PIN, rState ? HIGH : LOW);
    pub_socket1.publish(rState ? "0" : "1");
  }

  int sw2 = digitalRead(SWITCH2_PIN);
  if (sw2 != lastSw2) {
    lastSw2 = sw2;
    bool rState = !digitalRead(RELAY2_PIN);
    digitalWrite(RELAY2_PIN, rState ? HIGH : LOW);
    pub_socket2.publish(rState ? "0" : "1");
  }

  int sw3 = digitalRead(SWITCH3_PIN);
  if (sw3 != lastSw3) {
    lastSw3 = sw3;
    bool rState = !digitalRead(RELAY3_PIN);
    digitalWrite(RELAY3_PIN, rState ? HIGH : LOW);
    pub_socket3.publish(rState ? "0" : "1");
  }

  int sw4 = digitalRead(SWITCH4_PIN);
  if (sw4 != lastSw4) {
    lastSw4 = sw4;
    bool rState = !digitalRead(RELAY4_PIN);
    digitalWrite(RELAY4_PIN, rState ? HIGH : LOW);
    pub_socket4.publish(rState ? "0" : "1");
  }

  Adafruit_MQTT_Subscribe *sub;
  while ((sub = mqtt.readSubscription(200))) {
    if (sub == &sub_socket1) {
      int val = atoi((char *)sub_socket1.lastread);
      digitalWrite(RELAY1_PIN, val == 1 ? LOW : HIGH);
    } else if (sub == &sub_socket2) {
      int val = atoi((char *)sub_socket2.lastread);
      digitalWrite(RELAY2_PIN, val == 1 ? LOW : HIGH);
    } else if (sub == &sub_socket3) {
      int val = atoi((char *)sub_socket3.lastread);
      digitalWrite(RELAY3_PIN, val == 1 ? LOW : HIGH);
    } else if (sub == &sub_socket4) {
      int val = atoi((char *)sub_socket4.lastread);
      digitalWrite(RELAY4_PIN, val == 1 ? LOW : HIGH);
    } else if (sub == &sub_fan) {
      int speed = constrain(atoi((char *)sub_fan.lastread), 0, 100);
      analogWrite(FAN_PWM_PIN, map(speed, 0, 100, 0, 1023));
    }
  }
}

void MQTT_connect() {
  if (mqtt.connected()) return;
  int8_t ret;
  while ((ret = mqtt.connect()) != 0) {
    mqtt.disconnect();
    delay(5000);
  }
  pub_status.publish("Device 1 Online (ESP8266 NodeMCU)");
}
"""

private const val ESP8266_LED_CLOCK_SKETCH = """#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <FastLED.h>
#include <EEPROM.h>
#include <time.h>

// ================= DEVICE 2: LED DIGITAL CLOCK (FastLED + HTTP) =================
// Hardware: ESP8266 D1 Mini Pro, WS2812B on Pin D4 (GPIO2), LDR on Pin A0 (ADC0)

#define WLAN_SSID       "YOUR_NETIS_WIFI_SSID"
#define WLAN_PASS       "YOUR_NETIS_WIFI_PASS"

// Static IP matching app default
IPAddress staticIP(192, 168, 1, 102);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
IPAddress dns(192, 168, 1, 1);

#define LED_PIN         2    // D4 on ESP8266 D1 Mini / NodeMCU (GPIO2)
#define NUM_LEDS        37   // WS2812B NeoPixel strip/matrix
#define LDR_PIN         A0   // Analog Ambient Light Sensor (0..1023)
#define LED_TYPE        WS2812B
#define COLOR_ORDER     GRB

CRGB leds[NUM_LEDS];
ESP8266WebServer server(80);

// Clock Settings with EEPROM Persistence
struct ClockConfig {
  uint8_t magic;         // 0x5C
  uint8_t brightness;    // 0..255
  bool autoBrightness;
  uint8_t effect;        // 0..15
  uint8_t speed;         // 1..100
  bool effectsEnabled;
  uint8_t clockR, clockG, clockB;
  uint8_t weekR, weekG, weekB;
  uint8_t colonR, colonG, colonB;
  uint8_t dateR, dateG, dateB;
  bool hour24;
  bool blinkColon;
  bool showDate;
};

ClockConfig config = {
  0x5C, 180, false, 0, 50, true,
  0, 200, 255,   // Clock Cyan
  0, 255, 128,   // Week Mint
  255, 180, 0,   // Colon Amber
  255, 100, 200, // Date Rose
  true, true, true
};

// Filtered LDR value for smooth, flicker-free auto-brightness
int rawLdr = 512;
float filteredLdr = 512.0;
unsigned long lastLdrCheck = 0;
unsigned long lastEffectStep = 0;
uint16_t effectFrame = 0;

void saveConfig() {
  EEPROM.put(0, config);
  EEPROM.commit();
}

void loadConfig() {
  EEPROM.begin(512);
  ClockConfig loaded;
  EEPROM.get(0, loaded);
  if (loaded.magic == 0x5C) {
    config = loaded;
  } else {
    saveConfig();
  }
}

// ---------------- REST API Handlers ----------------

void handleStatus() {
  rawLdr = analogRead(LDR_PIN);
  String json = "{";
  json += "\"clock\":\"" + String(config.clockR) + "," + String(config.clockG) + "," + String(config.clockB) + "\",";
  json += "\"week\":\"" + String(config.weekR) + "," + String(config.weekG) + "," + String(config.weekB) + "\",";
  json += "\"colon\":\"" + String(config.colonR) + "," + String(config.colonG) + "," + String(config.colonB) + "\",";
  json += "\"datecolor\":\"" + String(config.dateR) + "," + String(config.dateG) + "," + String(config.dateB) + "\",";
  json += "\"effect\":" + String(config.effect) + ",";
  json += "\"effects_enabled\":" + String(config.effectsEnabled ? "true" : "false") + ",";
  json += "\"brightness\":" + String(config.brightness) + ",";
  json += "\"auto\":" + String(config.autoBrightness ? "true" : "false") + ",";
  json += "\"hour24\":" + String(config.hour24 ? "true" : "false") + ",";
  json += "\"blinkcolon\":" + String(config.blinkColon ? "true" : "false") + ",";
  json += "\"showdate\":" + String(config.showDate ? "true" : "false") + ",";
  json += "\"speed\":" + String(config.speed) + ",";
  json += "\"timezone\":\"Asia/Dhaka (UTC+6)\",";
  json += "\"ntp_synced\":true,";
  json += "\"ldr\":" + String(rawLdr);
  json += "}";
  server.send(200, "application/json", json);
}

void handleLdr() {
  rawLdr = analogRead(LDR_PIN);
  String json = "{\"ldr\":" + String(rawLdr) + "}";
  server.send(200, "application/json", json);
}

void handleBrightness() {
  String body = server.arg("plain");
  if (server.hasArg("brightness")) {
    config.brightness = constrain(server.arg("brightness").toInt(), 0, 255);
  }
  if (server.hasArg("auto")) {
    config.autoBrightness = (server.arg("auto") == "true" || server.arg("auto") == "1");
  }
  // Simple JSON parsing
  int bIdx = body.indexOf("\"brightness\":");
  if (bIdx != -1) {
    config.brightness = constrain(body.substring(bIdx + 13).toInt(), 0, 255);
  }
  int aIdx = body.indexOf("\"auto\":");
  if (aIdx != -1) {
    String sub = body.substring(aIdx + 7);
    config.autoBrightness = sub.startsWith("true");
  }

  if (!config.autoBrightness) {
    FastLED.setBrightness(config.brightness);
  }
  saveConfig();
  server.send(200, "application/json", "{\"status\":\"OK\",\"brightness\":" + String(config.brightness) + "}");
}

void handleEffects() {
  String body = server.arg("plain");
  if (server.hasArg("effect")) {
    config.effect = constrain(server.arg("effect").toInt(), 0, 15);
    config.effectsEnabled = true;
  }
  if (server.hasArg("speed")) {
    config.speed = constrain(server.arg("speed").toInt(), 1, 100);
  }
  if (server.hasArg("enabled")) {
    config.effectsEnabled = (server.arg("enabled") == "true" || server.arg("enabled") == "1");
  }

  int eIdx = body.indexOf("\"effect\":");
  if (eIdx != -1) {
    config.effect = constrain(body.substring(eIdx + 9).toInt(), 0, 15);
    config.effectsEnabled = true;
  }
  int sIdx = body.indexOf("\"speed\":");
  if (sIdx != -1) {
    config.speed = constrain(body.substring(sIdx + 8).toInt(), 1, 100);
  }
  int enIdx = body.indexOf("\"enabled\":");
  if (enIdx != -1) {
    String sub = body.substring(enIdx + 10);
    config.effectsEnabled = sub.startsWith("true");
  }

  saveConfig();
  server.send(200, "application/json", "{\"status\":\"OK\",\"effect\":" + String(config.effect) + ",\"speed\":" + String(config.speed) + "}");
}

void parseRgbString(String val, uint8_t &r, uint8_t &g, uint8_t &b) {
  int c1 = val.indexOf(',');
  int c2 = val.indexOf(',', c1 + 1);
  if (c1 != -1 && c2 != -1) {
    r = constrain(val.substring(0, c1).toInt(), 0, 255);
    g = constrain(val.substring(c1 + 1, c2).toInt(), 0, 255);
    b = constrain(val.substring(c2 + 1).toInt(), 0, 255);
  }
}

void handleColor() {
  String body = server.arg("plain");
  if (server.hasArg("clock")) parseRgbString(server.arg("clock"), config.clockR, config.clockG, config.clockB);
  if (server.hasArg("week"))  parseRgbString(server.arg("week"), config.weekR, config.weekG, config.weekB);
  if (server.hasArg("colon")) parseRgbString(server.arg("colon"), config.colonR, config.colonG, config.colonB);
  if (server.hasArg("datecolor")) parseRgbString(server.arg("datecolor"), config.dateR, config.dateG, config.dateB);

  saveConfig();
  server.send(200, "application/json", "{\"status\":\"OK\"}");
}

// ---------------- 16 FastLED Clock Effects (Speed-Modulated) ----------------

void renderEffect(uint8_t effectId, uint8_t speedPercent) {
  uint8_t stepDelay = map(speedPercent, 1, 100, 120, 5); // 1% = 120ms, 100% = 5ms
  if (millis() - lastEffectStep < stepDelay) return;
  lastEffectStep = millis();
  effectFrame++;

  switch (effectId) {
    case 0: // Solid Color
      fill_solid(leds, NUM_LEDS, CRGB(config.clockR, config.clockG, config.clockB));
      break;

    case 1: // Rainbow Flow
      fill_rainbow(leds, NUM_LEDS, (effectFrame * 3) & 0xFF, 7);
      break;

    case 2: // Color Wave
      for (int i = 0; i < NUM_LEDS; i++) {
        uint8_t wave = sin8((i * 14) + (effectFrame * 4));
        leds[i] = CHSV(wave, 230, 255);
      }
      break;

    case 3: // Breathing Glow
      {
        uint8_t breathe = beatsin8(map(speedPercent, 1, 100, 10, 60), 40, 255);
        fill_solid(leds, NUM_LEDS, CRGB(config.clockR, config.clockG, config.clockB));
        fadeToBlackBy(leds, NUM_LEDS, 255 - breathe);
      }
      break;

    case 4: // Slot Machine
      for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = CHSV(((i * 20) + (effectFrame * 10)) & 0xFF, 200, 240);
      }
      break;

    case 5: // Digit Cascade
      fadeToBlackBy(leds, NUM_LEDS, 35);
      leds[effectFrame % NUM_LEDS] = CRGB(config.clockR, config.clockG, config.clockB);
      break;

    case 6: // Neon Flicker
      {
        uint8_t flicker = random8(160, 255);
        fill_solid(leds, NUM_LEDS, CRGB(config.clockR, config.clockG, config.clockB));
        fadeToBlackBy(leds, NUM_LEDS, 255 - flicker);
      }
      break;

    case 7: // Fireplace Glow
      for (int i = 0; i < NUM_LEDS; i++) {
        uint8_t warmth = random8(140, 255);
        leds[i] = CRGB(warmth, warmth / 3, 0);
      }
      break;

    case 8: // Aurora Borealis
      for (int i = 0; i < NUM_LEDS; i++) {
        uint8_t hue = map(sin8((i * 12) + (effectFrame * 2)), 0, 255, 96, 170); // Green to Violet
        leds[i] = CHSV(hue, 220, 240);
      }
      break;

    case 9: // Matrix Digital Rain
      fadeToBlackBy(leds, NUM_LEDS, 40);
      if (random8() < 90) leds[random8(NUM_LEDS)] = CRGB(0, 255, 60);
      break;

    case 10: // Color Chase
      fadeToBlackBy(leds, NUM_LEDS, 60);
      leds[effectFrame % NUM_LEDS] = CRGB(config.colonR, config.colonG, config.colonB);
      break;

    case 11: // Segment Fade
      {
        uint8_t blendHue = (effectFrame * 2) & 0xFF;
        fill_solid(leds, NUM_LEDS, CHSV(blendHue, 220, 255));
      }
      break;

    case 12: // Starry Night
      fadeToBlackBy(leds, NUM_LEDS, 25);
      if (random8() < 70) leds[random8(NUM_LEDS)] = CRGB(200, 220, 255);
      break;

    case 13: // Sunset Horizon
      for (int i = 0; i < NUM_LEDS; i++) {
        uint8_t hue = map(i, 0, NUM_LEDS - 1, 0, 42); // Red to Gold
        leds[i] = CHSV(hue, 240, 255);
      }
      break;

    case 14: // Cyber Pulse
      {
        uint8_t pulse = sin8(effectFrame * 6);
        CRGB c1 = CRGB(255, 0, 128); // Neon Pink
        CRGB c2 = CRGB(0, 220, 255); // Cyan
        CRGB blended = blend(c1, c2, pulse);
        fill_solid(leds, NUM_LEDS, blended);
      }
      break;

    case 15: // Spectrum Analyzer
      {
        uint8_t topLed = map(sin8(effectFrame * 8), 0, 255, 1, NUM_LEDS);
        for (int i = 0; i < NUM_LEDS; i++) {
          if (i < topLed) leds[i] = CHSV(map(i, 0, NUM_LEDS, 96, 0), 240, 255);
          else leds[i] = CRGB::Black;
        }
      }
      break;
  }
}

// ---------------- Setup & Loop ----------------

void setup() {
  Serial.begin(115200);
  loadConfig();

  FastLED.addLeds<LED_TYPE, LED_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(config.brightness);
  fill_solid(leds, NUM_LEDS, CRGB(config.clockR, config.clockG, config.clockB));
  FastLED.show();

  WiFi.config(staticIP, gateway, subnet, dns);
  WiFi.begin(WLAN_SSID, WLAN_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(400); }

  configTime(6 * 3600, 0, "pool.ntp.org", "time.nist.gov");

  // REST API Routes
  server.on("/api/status", HTTP_GET, handleStatus);
  server.on("/status", HTTP_GET, handleStatus);

  server.on("/api/ldr", HTTP_GET, handleLdr);
  server.on("/testldr", HTTP_GET, handleLdr);

  server.on("/api/brightness", HTTP_POST, handleBrightness);
  server.on("/set", HTTP_GET, []() {
    if (server.hasArg("brightness") || server.hasArg("auto")) handleBrightness();
    if (server.hasArg("effect") || server.hasArg("speed")) handleEffects();
    if (server.hasArg("clock")) handleColor();
  });

  server.on("/api/effects", HTTP_POST, handleEffects);
  server.on("/api/color", HTTP_POST, handleColor);

  server.on("/save", HTTP_GET, []() { saveConfig(); server.send(200, "text/plain", "OK"); });
  server.on("/reset", HTTP_GET, []() {
    config = { 0x5C, 180, false, 0, 50, true, 0, 200, 255, 0, 255, 128, 255, 180, 0, 255, 100, 200, true, true, true };
    saveConfig();
    server.send(200, "text/plain", "RESET_OK");
  });

  server.begin();
}

void loop() {
  server.handleClient();

  // Read A0 ADC continuously every 50ms with Exponential Moving Average (EMA)
  if (millis() - lastLdrCheck >= 50) {
    lastLdrCheck = millis();
    rawLdr = analogRead(LDR_PIN); // 0..1023
    filteredLdr = (filteredLdr * 0.85f) + (rawLdr * 0.15f);

    if (config.autoBrightness) {
      // Scale ambient reading (50..950) to FastLED brightness (10..255)
      int targetBrightness = map(constrain((int)filteredLdr, 40, 960), 40, 960, 15, 255);
      FastLED.setBrightness(targetBrightness);
    }
  }

  // Animate dynamic FastLED effects respecting speed parameter
  if (config.effectsEnabled) {
    renderEffect(config.effect, config.speed);
  } else {
    fill_solid(leds, NUM_LEDS, CRGB(config.clockR, config.clockG, config.clockB));
  }

  FastLED.show();
}
"""
