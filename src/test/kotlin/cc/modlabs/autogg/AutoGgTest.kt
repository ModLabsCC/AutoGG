package cc.modlabs.autogg

import java.nio.file.Files
import java.io.IOException
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

    @Test
    fun `server overrides survive reload and use most specific domain`() {
        val path = Files.createTempDirectory("autogg-overrides").resolve("autogg.json")
        val settings = AutoGgSettings(path)
        settings.save("default gg")
        settings.saveServer("hypixel.net", listOf("gg", "well played"))
        settings.saveServer("modlabs.cc", listOf("nice"))
        settings.saveServer("play.modlabs.cc", listOf("great game"))
        val reloaded = AutoGgSettings(path)
        assertEquals(listOf("gg", "well played"), reloaded.messagesFor("MC.HYPIXEL.NET:25565"))
        assertEquals(listOf("nice"), reloaded.messagesFor("modlabs.cc"))
        assertEquals(listOf("great game"), reloaded.messagesFor("play.modlabs.cc"))
        assertEquals(listOf("default gg"), reloaded.messagesFor("notmodlabs.cc"))
        reloaded.saveServer("modlabs.cc", emptyList())
        assertEquals(listOf("default gg"), AutoGgSettings(path).messagesFor("modlabs.cc"))
    }

    @Test
    fun `hypixel list picks a different message when possible`() {
        val guard = AutoGgGuard()
        guard.sent("gg")
        assertEquals("nice game", guard.chooseMessage(ServerConfig.HYPIXEL, listOf("gg", "nice game")))
        assertEquals(null, guard.chooseMessage(ServerConfig.HYPIXEL, listOf("gg")))
    }

    @Test
    fun `old text setting is preserved when json is created`() {
        val directory = Files.createTempDirectory("autogg-migration")
        Files.writeString(directory.resolve("autogg-message.txt"), "legacy message")
        val path = directory.resolve("autogg.json")
        val settings = AutoGgSettings(path)
        assertEquals("legacy message", settings.message)
        settings.saveServer("modlabs.cc", listOf("nice"))
        assertEquals("legacy message", AutoGgSettings(path).message)
    }

    @Test
    fun `message editor preserves separators and backslashes`() {
        val messages = listOf("gg | wp", "nice \\ game")
        assertEquals(messages, parseMessages(formatMessages(messages)))
        assertFailsWith<IllegalArgumentException> { parseMessages("gg\\") }
    }

    @Test
    fun `server detection respects host boundaries case and port`() {
        assertTrue(ServerConfig.HYPIXEL.matches("MC.HYPIXEL.NET:25565"))
        assertFalse(ServerConfig.HYPIXEL.matches("hypixel.net.example.org"))
        assertFalse(ServerConfig.HYPIXEL.matches("nothypixel.net"))
    }

    @Test
    fun `invalid stored configuration is not overwritten`() {
        val path = Files.createTempDirectory("autogg-broken").resolve("autogg.json")
        Files.writeString(path, "{ broken")
        val settings = AutoGgSettings(path)
        assertFailsWith<IOException> { settings.save("nice") }
        assertEquals("{ broken", Files.readString(path))
    }
}
