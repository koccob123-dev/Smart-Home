package com.example.data

import com.example.model.ClockColor
import com.example.model.ClockStatus
import org.json.JSONObject

class ClockRepository(
    private val clockApi: ClockApi = ClockApi()
) {
    suspend fun getStatus(ip: String): Result<ClockStatus> {
        return clockApi.getStatus(ip)
    }

    suspend fun getLdr(ip: String): Result<Int> {
        return clockApi.getLdr(ip)
    }

    suspend fun testLdr(ip: String): Result<Int> {
        return clockApi.getLdr(ip)
    }

    suspend fun setClockColor(ip: String, color: ClockColor): Result<String> {
        val json = JSONObject().apply {
            put("clock", color.toParamString())
            put("color", color.toParamString())
        }
        return clockApi.postJson(ip, "/api/color", json, fallbackGetQuery = "/set?clock=${color.toParamString()}")
    }

    suspend fun setDigitColor(ip: String, digitIndex: Int, color: ClockColor): Result<String> {
        val json = JSONObject().apply {
            put("digit", digitIndex)
            put("color", color.toParamString())
        }
        return clockApi.postJson(ip, "/api/color", json, fallbackGetQuery = "/set?digit$digitIndex=${color.toParamString()}")
    }

    suspend fun setWeekColor(ip: String, color: ClockColor): Result<String> {
        val json = JSONObject().apply {
            put("week", color.toParamString())
        }
        return clockApi.postJson(ip, "/api/color", json, fallbackGetQuery = "/set?week=${color.toParamString()}")
    }

    suspend fun setColonColor(ip: String, color: ClockColor): Result<String> {
        val json = JSONObject().apply {
            put("colon", color.toParamString())
        }
        return clockApi.postJson(ip, "/api/color", json, fallbackGetQuery = "/set?colon=${color.toParamString()}")
    }

    suspend fun setDateColor(ip: String, color: ClockColor): Result<String> {
        val json = JSONObject().apply {
            put("datecolor", color.toParamString())
        }
        return clockApi.postJson(ip, "/api/color", json, fallbackGetQuery = "/set?datecolor=${color.toParamString()}")
    }

    suspend fun setEffect(ip: String, effectId: Int): Result<String> {
        val json = JSONObject().apply {
            put("effect", effectId)
        }
        return clockApi.postJson(ip, "/api/effects", json, fallbackGetQuery = "/set?effect=$effectId")
    }

    suspend fun setEffectsEnabled(ip: String, enabled: Boolean): Result<String> {
        val json = JSONObject().apply {
            put("enabled", enabled)
        }
        return clockApi.postJson(ip, "/api/effects", json, fallbackGetQuery = if (enabled) "/set?effect=1" else "/set?effect=0")
    }

    suspend fun setSpeed(ip: String, speed: Int): Result<String> {
        val json = JSONObject().apply {
            put("speed", speed.coerceIn(1, 100))
        }
        return clockApi.postJson(ip, "/api/effects", json, fallbackGetQuery = "/set?speed=${speed.coerceIn(1, 100)}")
    }

    suspend fun setBrightness(ip: String, brightness: Int): Result<String> {
        val json = JSONObject().apply {
            put("brightness", brightness.coerceIn(0, 255))
        }
        return clockApi.postJson(ip, "/api/brightness", json, fallbackGetQuery = "/set?brightness=${brightness.coerceIn(0, 255)}")
    }

    suspend fun setAutoBrightness(ip: String, enabled: Boolean): Result<String> {
        val json = JSONObject().apply {
            put("auto", enabled)
            put("auto_brightness", enabled)
        }
        return clockApi.postJson(ip, "/api/brightness", json, fallbackGetQuery = "/set?auto=$enabled")
    }

    suspend fun setHour24(ip: String, is24Hour: Boolean): Result<String> {
        val json = JSONObject().apply {
            put("hour24", is24Hour)
        }
        return clockApi.postJson(ip, "/api/time-format", json, fallbackGetQuery = "/set?hour24=$is24Hour")
    }

    suspend fun setBlinkColon(ip: String, blink: Boolean): Result<String> {
        val json = JSONObject().apply {
            put("blinkcolon", blink)
        }
        return clockApi.postJson(ip, "/api/settings", json, fallbackGetQuery = "/set?blinkcolon=$blink")
    }

    suspend fun setShowDate(ip: String, show: Boolean): Result<String> {
        val json = JSONObject().apply {
            put("showdate", show)
        }
        return clockApi.postJson(ip, "/api/settings", json, fallbackGetQuery = "/set?showdate=$show")
    }

    suspend fun triggerNtpSync(ip: String): Result<String> {
        val json = JSONObject().apply {
            put("timezone", "Asia/Dhaka")
            put("utc_offset", 6)
        }
        return clockApi.postJson(ip, "/api/ntp-sync", json, fallbackGetQuery = "/sync")
    }

    suspend fun saveSettings(ip: String): Result<String> {
        val json = JSONObject().apply { put("action", "save") }
        return clockApi.postJson(ip, "/api/settings", json, fallbackGetQuery = "/save")
    }

    suspend fun resetSettings(ip: String): Result<String> {
        val json = JSONObject().apply { put("action", "reset") }
        return clockApi.postJson(ip, "/api/settings", json, fallbackGetQuery = "/reset")
    }
}
