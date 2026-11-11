package com.example.data

import android.util.Log
import com.example.model.ClockColor
import com.example.model.ClockStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ClockApi: HTTP client for ESP8266 LED Digital Clock.
 * Endpoints matched with ESP8266 firmware:
 * - GET  /api/status     -> JSON with clock state, brightness, effect, speed, ldr (0-1023)
 * - POST /api/effects    -> JSON: {"effect": 0..15, "speed": 1..100, "enabled": true/false}
 * - POST /api/brightness -> JSON: {"brightness": 0..255, "auto": true/false}
 * - GET  /api/ldr        -> JSON: {"ldr": 0..1023} or raw ADC integer
 * - POST /api/color      -> JSON: {"clock": "R,G,B", ...}
 * - POST /api/time-format-> JSON: {"hour24": true/false}
 * - POST /api/ntp-sync   -> JSON: {"timezone": "Asia/Dhaka", "utc_offset": 6}
 * - POST /api/settings   -> JSON: {"blinkcolon": ..., "showdate": ..., "action": "save"|"reset"}
 * With fallback to classic /status, /set?..., /testldr query parameters.
 */
class ClockApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3000, TimeUnit.MILLISECONDS)
        .readTimeout(3000, TimeUnit.MILLISECONDS)
        .writeTimeout(3000, TimeUnit.MILLISECONDS)
        .build()
) {
    private val tag = "ClockApi"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(ip: String, path: String): String {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        return "http://$cleanIp$path"
    }

    suspend fun getStatus(ip: String): Result<ClockStatus> = withContext(Dispatchers.IO) {
        // Try GET /api/status first
        val primaryUrl = buildUrl(ip, "/api/status")
        try {
            val req = Request.Builder().url(primaryUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    return@withContext Result.success(parseStatus(ip, body))
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "GET /api/status failed, attempting fallback to /status: ${e.message}")
        }

        // Fallback to /status
        val fallbackUrl = buildUrl(ip, "/status")
        val request = Request.Builder().url(fallbackUrl).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                val body = response.body?.string().orEmpty()
                Result.success(parseStatus(ip, body))
            }
        } catch (e: Exception) {
            Log.e(tag, "getStatus failed for $ip", e)
            Result.failure(e)
        }
    }

    suspend fun getLdr(ip: String): Result<Int> = withContext(Dispatchers.IO) {
        // Try GET /api/ldr first
        val primaryUrl = buildUrl(ip, "/api/ldr")
        try {
            val req = Request.Builder().url(primaryUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val ldrVal = parseLdrResponse(body)
                    if (ldrVal != null) {
                        return@withContext Result.success(ldrVal)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "GET /api/ldr failed, trying /testldr: ${e.message}")
        }

        // Fallback to /testldr
        val fallbackUrl = buildUrl(ip, "/testldr")
        try {
            val request = Request.Builder().url(fallbackUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                val body = response.body?.string().orEmpty()
                val ldrVal = parseLdrResponse(body)
                    ?: return@withContext Result.failure(IOException("Invalid LDR response: $body"))
                Result.success(ldrVal)
            }
        } catch (e: Exception) {
            Log.e(tag, "getLdr failed for $ip", e)
            Result.failure(e)
        }
    }

    private fun parseLdrResponse(body: String): Int? {
        val trimmed = body.trim()
        if (trimmed.startsWith("{")) {
            try {
                val json = JSONObject(trimmed)
                if (json.has("ldr")) return json.getInt("ldr").coerceIn(0, 1023)
                if (json.has("value")) return json.getInt("value").coerceIn(0, 1023)
                if (json.has("raw")) return json.getInt("raw").coerceIn(0, 1023)
            } catch (e: Exception) {
                Log.w(tag, "Error parsing LDR JSON", e)
            }
        }
        // Plain text parsing: e.g. "ldr: 485" or "485"
        val digits = trimmed.substringAfter("ldr:").substringAfter("LDR:").trim()
            .takeWhile { it.isDigit() }
        return digits.toIntOrNull()?.coerceIn(0, 1023)
    }

    suspend fun postJson(
        ip: String,
        endpoint: String,
        json: JSONObject,
        fallbackGetQuery: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val postUrl = buildUrl(ip, endpoint)
        val body = json.toString().toRequestBody(jsonMediaType)
        val postReq = Request.Builder().url(postUrl).post(body).build()

        try {
            client.newCall(postReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respBody = resp.body?.string().orEmpty()
                    return@withContext Result.success(respBody.ifBlank { "OK" })
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "POST $endpoint failed ($e), attempting fallback query...")
        }

        // If fallback query parameter provided
        if (!fallbackGetQuery.isNullOrBlank()) {
            val getUrl = buildUrl(ip, fallbackGetQuery)
            val getReq = Request.Builder().url(getUrl).get().build()
            try {
                client.newCall(getReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("HTTP ${response.code}: ${response.message}")
                        )
                    }
                    val respBody = response.body?.string().orEmpty()
                    return@withContext Result.success(respBody.ifBlank { "OK" })
                }
            } catch (e: Exception) {
                Log.e(tag, "Fallback command failed for $ip $fallbackGetQuery", e)
                return@withContext Result.failure(e)
            }
        }

        Result.failure(IOException("Failed to connect to $ip:80"))
    }

    suspend fun sendCommand(ip: String, pathWithQuery: String): Result<String> = withContext(Dispatchers.IO) {
        val url = buildUrl(ip, pathWithQuery)
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                val body = response.body?.string().orEmpty()
                Result.success(body.ifBlank { "OK" })
            }
        } catch (e: Exception) {
            Log.e(tag, "Command failed for $ip $pathWithQuery", e)
            Result.failure(e)
        }
    }

    private fun parseStatus(ip: String, raw: String): ClockStatus {
        try {
            if (raw.trim().startsWith("{")) {
                val json = JSONObject(raw)
                val clockRgb = json.optString("clock", json.optString("color", "0,200,255"))
                val weekRgb = json.optString("week", "0,255,128")
                val colonRgb = json.optString("colon", "255,180,0")
                val dateRgb = json.optString("datecolor", json.optString("date_color", "255,100,200"))
                val effect = json.optInt("effect", json.optInt("effect_id", 0))
                val effectsEnabled = json.optBoolean("effects_enabled", json.optBoolean("enabled", true))
                val brightness = json.optInt("brightness", 180).coerceIn(0, 255)
                val auto = json.optBoolean("auto", json.optBoolean("auto_brightness", false))
                val hour24 = json.optBoolean("hour24", true)
                val blinkcolon = json.optBoolean("blinkcolon", true)
                val showdate = json.optBoolean("showdate", true)
                val speed = json.optInt("speed", 50).coerceIn(1, 100)
                val timezone = json.optString("timezone", "Asia/Dhaka (UTC+6)")
                val ntpSynced = json.optBoolean("ntp_synced", true)
                val ldr = if (json.has("ldr")) json.optInt("ldr").coerceIn(0, 1023) else null

                val baseColor = ClockColor.parse(clockRgb) ?: ClockColor(0, 200, 255)

                return ClockStatus(
                    clockColor = baseColor,
                    weekColor = ClockColor.parse(weekRgb) ?: ClockColor(0, 255, 128),
                    colonColor = ClockColor.parse(colonRgb) ?: ClockColor(255, 180, 0),
                    dateColor = ClockColor.parse(dateRgb) ?: ClockColor(255, 100, 200),
                    digit1Color = ClockColor.parse(json.optString("digit1")) ?: baseColor,
                    digit2Color = ClockColor.parse(json.optString("digit2")) ?: baseColor,
                    digit3Color = ClockColor.parse(json.optString("digit3")) ?: baseColor,
                    digit4Color = ClockColor.parse(json.optString("digit4")) ?: baseColor,
                    effect = effect,
                    effectsEnabled = effectsEnabled,
                    brightness = brightness,
                    autoBrightness = auto,
                    hour24 = hour24,
                    blinkColon = blinkcolon,
                    showDate = showdate,
                    speed = speed,
                    timezone = timezone,
                    ntpSynced = ntpSynced,
                    ldrRawValue = ldr,
                    ip = ip,
                    isOnline = true,
                    lastResponse = raw.take(120)
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Status was not pure JSON, parsing key-value pairs: ${e.message}")
        }

        // Fallback key-value parser for simple ESP8266 plain text responses
        var clockC = ClockColor(0, 200, 255)
        var weekC = ClockColor(0, 255, 128)
        var colonC = ClockColor(255, 180, 0)
        var dateC = ClockColor(255, 100, 200)
        var effect = 0
        var brightness = 180
        var auto = false
        var hour24 = true
        var blinkcolon = true
        var showdate = true
        var speed = 50
        var ldr: Int? = null

        raw.lines().forEach { line ->
            val parts = line.split("=", ":")
            if (parts.size >= 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "clock", "color" -> ClockColor.parse(value)?.let { clockC = it }
                    "week" -> ClockColor.parse(value)?.let { weekC = it }
                    "colon" -> ClockColor.parse(value)?.let { colonC = it }
                    "datecolor", "date" -> ClockColor.parse(value)?.let { dateC = it }
                    "effect" -> effect = value.toIntOrNull() ?: effect
                    "brightness" -> brightness = (value.toIntOrNull() ?: brightness).coerceIn(0, 255)
                    "auto" -> auto = value.toBooleanStrictOrNull() ?: (value == "1")
                    "hour24" -> hour24 = value.toBooleanStrictOrNull() ?: (value == "1")
                    "blinkcolon" -> blinkcolon = value.toBooleanStrictOrNull() ?: (value == "1")
                    "showdate" -> showdate = value.toBooleanStrictOrNull() ?: (value == "1")
                    "speed" -> speed = (value.toIntOrNull() ?: speed).coerceIn(1, 100)
                    "ldr" -> ldr = value.toIntOrNull()?.coerceIn(0, 1023)
                }
            }
        }

        return ClockStatus(
            clockColor = clockC,
            weekColor = weekC,
            colonColor = colonC,
            dateColor = dateC,
            digit1Color = clockC,
            digit2Color = clockC,
            digit3Color = clockC,
            digit4Color = clockC,
            effect = effect,
            brightness = brightness,
            autoBrightness = auto,
            hour24 = hour24,
            blinkColon = blinkcolon,
            showDate = showdate,
            speed = speed,
            ldrRawValue = ldr,
            ip = ip,
            isOnline = true,
            lastResponse = raw.take(120)
        )
    }
}
