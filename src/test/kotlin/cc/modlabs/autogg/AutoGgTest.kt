package cc.modlabs.autogg

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AutoGgTest {
    @Test
    fun `hypixel repeated messages wait for a different chat message`() {
        val guard = AutoGgGuard()
        assertTrue(guard.shouldSend(ServerConfig.HYPIXEL, "gg", 1_000))
        guard.sent("gg")
        assertFalse(guard.shouldSend(ServerConfig.HYPIXEL, "gg", 10_000))
        guard.sent("well played")
        assertTrue(guard.shouldSend(ServerConfig.HYPIXEL, "gg", 20_000))
    }

    @Test
    fun `bursts of end messages are suppressed without blocking later matches`() {
        val guard = AutoGgGuard()
        assertTrue(guard.shouldSend(ServerConfig.PVPLAND, "gg", 1_000))
        guard.sent("gg")
        assertFalse(guard.shouldSend(ServerConfig.PVPLAND, "gg", 3_000))
        assertFalse(guard.shouldSend(ServerConfig.PVPLAND, "gg", 5_000))
        assertTrue(guard.shouldSend(ServerConfig.PVPLAND, "gg", 10_000))
    }

    @Test
    fun `message is saved loaded and invalid values cannot replace it`() {
        val path = Files.createTempDirectory("autogg-test").resolve("message.txt")
        val settings = AutoGgSettings(path)
        assertEquals("gg", settings.message)
        settings.save("well played!")
        assertEquals("well played!", AutoGgSettings(path).message)
        for (invalid in listOf(" ", "/command", "a\nb", "a".repeat(257))) {
            assertFailsWith<IllegalArgumentException> { settings.save(invalid) }
        }
        assertEquals("well played!", AutoGgSettings(path).message)
    }
}
