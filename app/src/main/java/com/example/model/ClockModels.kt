package com.example.model

data class ClockColor(
    val r: Int,
    val g: Int,
    val b: Int
) {
    fun toParamString(): String = "$r,$g,$b"

    fun toComposeColor(): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color(
            red = r.coerceIn(0, 255),
            green = g.coerceIn(0, 255),
            blue = b.coerceIn(0, 255)
        )
    }

    companion object {
        fun fromHex(hex: Long): ClockColor {
            val r = ((hex shr 16) and 0xFF).toInt()
            val g = ((hex shr 8) and 0xFF).toInt()
            val b = (hex and 0xFF).toInt()
            return ClockColor(r, g, b)
        }

        fun fromRgb(r: Int, g: Int, b: Int): ClockColor {
            return ClockColor(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        }

        fun parse(str: String?): ClockColor? {
            if (str.isNullOrBlank()) return null
            val parts = str.split(",").mapNotNull { it.trim().toIntOrNull() }
            return if (parts.size >= 3) {
                ClockColor(parts[0], parts[1], parts[2])
            } else {
                null
            }
        }
    }
}

data class ClockEffect(
    val id: Int,
    val name: String,
    val description: String
)

object ClockEffectsList {
    val ALL_EFFECTS = listOf(
        ClockEffect(0, "Solid Color", "Static single color on all display segments"),
        ClockEffect(1, "Rainbow Flow", "Smooth continuous rainbow gradient across digits"),
        ClockEffect(2, "Color Wave", "Gentle wave of alternating color hues"),
        ClockEffect(3, "Breathing Glow", "Pulsing brightness breathing in and out"),
        ClockEffect(4, "Slot Machine", "Numbers roll vertically before settling"),
        ClockEffect(5, "Digit Cascade", "Cascading transitions between time updates"),
        ClockEffect(6, "Neon Flicker", "Cyberpunk retro neon bulb pulsing glow"),
        ClockEffect(7, "Fireplace Glow", "Warm ambient flickering amber & orange flame"),
        ClockEffect(8, "Aurora Borealis", "Mystic shifting emerald, sky & violet waves"),
        ClockEffect(9, "Matrix Digital Rain", "Falling green cyber code accents"),
        ClockEffect(10, "Color Chase", "High speed traveling beam following segments"),
        ClockEffect(11, "Segment Fade", "Individual segment cross-fade transition"),
        ClockEffect(12, "Starry Night", "Twinkling star points around ambient digits"),
        ClockEffect(13, "Sunset Horizon", "Gradual blend from rich crimson to gold"),
        ClockEffect(14, "Cyber Pulse", "High-tech synthwave dual-hue color cycle"),
        ClockEffect(15, "Spectrum Analyzer", "Dynamic responsive equalizer style bounce")
    )
}

data class ClockStatus(
    val clockColor: ClockColor = ClockColor(0, 200, 255),
    val weekColor: ClockColor = ClockColor(0, 255, 128),
    val colonColor: ClockColor = ClockColor(255, 180, 0),
    val dateColor: ClockColor = ClockColor(255, 100, 200),
    // Per-digit color support
    val digit1Color: ClockColor = ClockColor(0, 200, 255),
    val digit2Color: ClockColor = ClockColor(0, 200, 255),
    val digit3Color: ClockColor = ClockColor(0, 200, 255),
    val digit4Color: ClockColor = ClockColor(0, 200, 255),
    val effect: Int = 0,
    val effectsEnabled: Boolean = true,
    val brightness: Int = 180,
    val autoBrightness: Boolean = false,
    val hour24: Boolean = true,
    val blinkColon: Boolean = true,
    val showDate: Boolean = true,
    val speed: Int = 50,
    val timezone: String = "Asia/Dhaka (UTC+6)",
    val ntpSynced: Boolean = true,
    val ldrRawValue: Int? = null,
    val ip: String = "192.168.1.102",
    val isOnline: Boolean = false,
    val lastResponse: String = ""
)

sealed class ClockConnectionState {
    data object Idle : ClockConnectionState()
    data class Testing(val ip: String) : ClockConnectionState()
    data class Connected(val ip: String, val timestamp: Long = System.currentTimeMillis()) : ClockConnectionState()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : ClockConnectionState()
}
