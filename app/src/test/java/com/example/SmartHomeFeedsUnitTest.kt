package com.example

import com.example.model.ClockColor
import com.example.model.ClockEffectsList
import com.example.model.FeedConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartHomeFeedsUnitTest {

    @Test
    fun verifyRelayAndSocketFeedKeys() {
        // Exact feeds required
        assertTrue(FeedConstants.ALL_FEEDS.contains("socket-1"))
        assertTrue(FeedConstants.ALL_FEEDS.contains("socket-2"))
        assertTrue(FeedConstants.ALL_FEEDS.contains("socket-3"))
        assertTrue(FeedConstants.ALL_FEEDS.contains("socket-4"))
        assertTrue(FeedConstants.ALL_FEEDS.contains("fan-speed"))
        assertTrue(FeedConstants.ALL_FEEDS.contains("device-status"))

        // Legacy aliases
        assertEquals("socket-1", FeedConstants.normalizeFeedKey("relay1"))
        assertEquals("socket-2", FeedConstants.normalizeFeedKey("relay2"))
        assertEquals("socket-3", FeedConstants.normalizeFeedKey("relay3"))
        assertEquals("socket-4", FeedConstants.normalizeFeedKey("relay4"))
        assertEquals("fan-speed", FeedConstants.normalizeFeedKey("fan-speed"))
    }

    @Test
    fun testTopicFormatting() {
        val username = "maker_user"
        val feedKey = FeedConstants.FEED_SOCKET_1
        val topic = FeedConstants.topicForFeed(username, feedKey)

        assertEquals("maker_user/feeds/socket-1", topic)
    }

    @Test
    fun testFeedExtractionFromTopic() {
        val topic = "maker_user/feeds/fan-speed"
        val extractedKey = FeedConstants.feedKeyFromTopic(topic)

        assertEquals("fan-speed", extractedKey)
    }

    @Test
    fun testClockColorParamString() {
        val color = ClockColor(255, 128, 64)
        assertEquals("255,128,64", color.toParamString())

        val parsed = ClockColor.parse("10,20,30")
        assertEquals(ClockColor(10, 20, 30), parsed)
    }

    @Test
    fun test16ClockEffectsPresence() {
        assertEquals(16, ClockEffectsList.ALL_EFFECTS.size)
        for (i in 0..15) {
            assertEquals(i, ClockEffectsList.ALL_EFFECTS[i].id)
        }
    }

    @Test
    fun testFanSpeedClamping() {
        val belowZero = -25
        val aboveHundred = 140
        val valid = 65

        assertEquals(0, belowZero.coerceIn(0, 100))
        assertEquals(100, aboveHundred.coerceIn(0, 100))
        assertEquals(65, valid.coerceIn(0, 100))
    }
}
