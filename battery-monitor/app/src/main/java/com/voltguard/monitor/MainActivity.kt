package com.voltguard.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Ink = Color(0xFF102B27)
private val Muted = Color(0xFF73817D)
private val Canvas = Color(0xFFF4F7F4)
private val Green = Color(0xFF0E8066)
private val Red = Color(0xFFC43C45)
private val Amber = Color(0xFFAF6C0B)
private val Line = Color(0xFFE5EBE7)

data class Reading(
    val voltage: Double? = null, val current: Double? = null,
    val soc: Double? = null, val temperature: Double? = null,
    val humidity: Double? = null, val inaOk: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("voltguard", MODE_PRIVATE)
        setContent {
            var host by remember { mutableStateOf(prefs.getString("host", "192.168.4.1") ?: "192.168.4.1") }
            var low by remember { mutableStateOf(prefs.getString("low", "11.8") ?: "11.8") }
            var high by remember { mutableStateOf(prefs.getString("high", "14.7") ?: "14.7") }
            var reading by remember { mutableStateOf(Reading()) }
            var connected by remember { mutableStateOf(false) }
            var refreshed by remember { mutableStateOf("") }
            var configured by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                while (isActive) {
                    val address = host.trim().removePrefix("http://").removeSuffix("/")
                    if (address.isNotBlank()) {
                        val result = withContext(Dispatchers.IO) { fetchReading(address) }
                        if (result != null) {
                            reading = result
                            connected = true
                            refreshed = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
                        } else connected = false
                    }
                    delay(2000)
                }
            }

            val v = reading.voltage
            val lowV = low.toDoubleOrNull() ?: 11.8
            val highV = high.toDoubleOrNull() ?: 14.7
            val alarm = if (v == null || !reading.inaOk) "Sensor unavailable" else when {
                v < lowV -> "UNDER-VOLTAGE"
                v > highV -> "OVER-VOLTAGE"
                else -> "Battery is healthy"
            }
            val statusColor = when (alarm) {
                "UNDER-VOLTAGE", "OVER-VOLTAGE" -> Red
                "Sensor unavailable" -> Amber
                else -> Green
            }

            MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Ink), Alignment.Center) {
                                        Icon(Icons.Rounded.BatteryChargingFull, null, tint = Color.White, modifier = Modifier.size(25.dp))
                                    }
                                    Column {
                                        Text("VOLTGUARD", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = 1.2.sp)
                                        Text("BATTERY MONITOR", fontSize = 10.sp, color = Muted, letterSpacing = 1.4.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) Green else Red))
                                    Text(if (connected) "LIVE" else "OFFLINE", color = if (connected) Green else Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                                }
                            }
                        }

                        item {
                            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Bolt, null, tint = Green, modifier = Modifier.size(18.dp))
                                        Text("BATTERY VOLTAGE", fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                                    }
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(v?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--.--", fontSize = 54.sp, lineHeight = 58.sp, fontWeight = FontWeight.SemiBold, color = Ink, letterSpacing = (-2).sp)
                                        Text(" V", fontSize = 19.sp, color = Muted, modifier = Modifier.padding(bottom = 7.dp))
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                        Text(alarm, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        item {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (alarm == "UNDER-VOLTAGE" || alarm == "OVER-VOLTAGE") Color(0xFFFFF1F0) else Color(0xFFEAF5F0)), elevation = CardDefaults.cardElevation(0.dp)) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(if (alarm == "UNDER-VOLTAGE" || alarm == "OVER-VOLTAGE") "!" else "✓", color = statusColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                    Column {
                                        Text(when (alarm) {
                                            "UNDER-VOLTAGE" -> "Battery below safe voltage"
                                            "OVER-VOLTAGE" -> "Battery above safe voltage"
                                            "Sensor unavailable" -> "No sensor data received"
                                            else -> "Voltage within safe range"
                                        }, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Low ${String.format(Locale.getDefault(), "%.1f", lowV)} V  ·  High ${String.format(Locale.getDefault(), "%.1f", highV)} V", color = Muted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Text("READINGS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricCard("CHARGE", reading.soc?.let { "${it.toInt()}%" } ?: "—", Icons.Rounded.BatteryChargingFull, Modifier.weight(1f))
                                MetricCard("CURRENT", reading.current?.let { String.format(Locale.getDefault(), "%.0f mA", it) } ?: "—", Icons.Rounded.Bolt, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricCard("TEMPERATURE", reading.temperature?.let { String.format(Locale.getDefault(), "%.1f°", it) } ?: "—", Icons.Rounded.Thermostat, Modifier.weight(1f))
                                MetricCard("HUMIDITY", reading.humidity?.let { "${it.toInt()}%" } ?: "—", Icons.Rounded.WaterDrop, Modifier.weight(1f))
                            }
                        }

                        item {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.CloudOff, null, tint = Muted, modifier = Modifier.size(17.dp))
                                        Text("ESP32 CONNECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink, letterSpacing = .7.sp)
                                    }
                                    OutlinedTextField(host, { host = it }, label = { Text("ESP32 IP address") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                                    if (!configured) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(low, { low = it }, label = { Text("Low V") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                            OutlinedTextField(high, { high = it }, label = { Text("High V") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                        }
                                    }
                                    Button(onClick = {
                                        prefs.edit().putString("host", host.trim()).putString("low", low).putString("high", high).apply()
                                        configured = !configured
                                    }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(if (configured) "Edit settings" else "Save settings")
                                    }
                                    Text(if (connected) "Updated $refreshed · auto-refresh 2 sec" else "Connect phone to the ESP32 Wi-Fi network", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            Text("12 V lead-acid preset · Confirm thresholds for your battery chemistry.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(19.dp))
            Text(value, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(label, color = Muted, fontSize = 9.sp, letterSpacing = .8.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun fetchReading(host: String): Reading? {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL("http://$host/api/status").openConnection() as HttpURLConnection).apply {
            connectTimeout = 1800
            readTimeout = 1800
            requestMethod = "GET"
        }
        if (connection.responseCode != 200) return null
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        fun number(key: String): Double? = if (json.isNull(key)) null else json.optDouble(key).takeIf { it.isFinite() }
        Reading(number("voltage"), number("current"), number("soc"), number("temperature"), number("humidity"), json.optBoolean("ina_ok", true))
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
