package cc.modlabs.autogg

import cc.modlabs.autogg.chat.Event
import cc.modlabs.autogg.chat.EventDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventDetectorTest {
    @Test
    fun `unknown servers detect only configured literal case-sensitive snippets`() {
        val detector = EventDetector()
        assertNull(detector.scanForEvent("Fixture end", null))
        assertEquals(Event.END_GAME, detector.scanForEvent("[Result] Fixture end!", null, listOf("Fixture end")))
        assertNull(detector.scanForEvent("fixture end", null, listOf("Fixture end")))
        assertNull(detector.scanForEvent("Fixture end", null, listOf("Fixture.*")))
    }

    @Test
    fun `custom lists replace built-ins and empty lists disable detection`() {
        val detector = EventDetector()
        assertEquals(Event.END_GAME, detector.scanForEvent("Winner: Alex", ServerConfig.HYPIXEL))
        assertNull(detector.scanForEvent("Winner: Alex", ServerConfig.HYPIXEL, listOf("Fixture end")))
        assertEquals(Event.END_GAME, detector.scanForEvent("Fixture end", ServerConfig.HYPIXEL, listOf("Fixture end")))
        assertNull(detector.scanForEvent("Winner: Alex", ServerConfig.HYPIXEL, emptyList()))
    }

    @Test
    fun `existing server markers retain their behavior`() {
        val detector = EventDetector()
        for ((server, message) in listOf(
            ServerConfig.HYPIXEL to "Your Overall Winstreak: 1",
            ServerConfig.BEDWARS_PRACTICE to "Game Lost!",
            ServerConfig.PVPLAND to "The match has ended!",
            ServerConfig.MINEMEN to "Match Results"
        )) {
            assertEquals(Event.END_GAME, detector.scanForEvent(message, server))
            assertNull(detector.scanForEvent("ordinary chat", server))
        }
        assertTrue(detector.builtInTriggers(ServerConfig.HYPIXEL).contains("Winner:"))
        assertEquals(emptyList(), detector.builtInTriggers(null))
    }

    @Test
    fun `mineplex state machine remains isolated from custom triggers and resets`() {
        val detector = EventDetector()
        val end = "Chat> Chat is no longer silenced."
        val server = ServerConfig.MINEPLEX
        assertEquals(Event.END_GAME, detector.scanForEvent("Fixture end", server, listOf("Fixture end")))
        assertEquals(Event.START_GAME, detector.scanForEvent(end, server))
        assertEquals(Event.END_GAME, detector.scanForEvent(end, server))
        assertNull(detector.scanForEvent("1st Place - Alex", server))
        assertEquals(Event.END_GAME, detector.scanForEvent(end, server))
        detector.scanForEvent("1st Place - Alex", server)
        detector.scanForEvent("You have been sent from Arena to Lobby", server)
        assertEquals(Event.START_GAME, detector.scanForEvent(end, server))
        detector.reset()
        assertEquals(Event.START_GAME, detector.scanForEvent(end, server))
    }

    @Test
    fun `custom matches retain burst and hypixel duplicate protection`() {
        val detector = EventDetector()
        val guard = AutoGgGuard()
        assertEquals(Event.END_GAME, detector.scanForEvent("Fixture end", null, listOf("Fixture end")))
        assertTrue(guard.shouldSend(null, "gg", 1000))
        guard.sent("gg")
        assertFalse(guard.shouldSend(null, "gg", 2000))
        assertTrue(guard.shouldSend(null, "gg", 6000))
        assertNull(guard.chooseMessage(ServerConfig.HYPIXEL, listOf("gg")))
    }
}
