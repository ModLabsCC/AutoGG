package cc.modlabs.autogg

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoGgEditorTest {
    private fun settings() = AutoGgSettings(Files.createTempDirectory("autogg-editor").resolve("autogg.json"))

    @Test
    fun `editor saves both fields and reloads the server settings`() {
        val settings = settings()
        val editor = AutoGgConfigEditor(settings)
        editor.load("example.net")
        editor.toggleMessages()
        editor.messages = "gg | well played"
        editor.toggleTriggers()
        editor.triggers = "Fixture end|Fixture winner"
        editor.save("EXAMPLE.NET.")
        val loaded = AutoGgConfigEditor(settings)
        loaded.load("example.net")
        assertTrue(loaded.customMessages)
        assertTrue(loaded.customTriggers)
        assertEquals(listOf("gg", "well played"), settings.messagesFor("example.net"))
        assertEquals(listOf("Fixture end", "Fixture winner"), settings.triggersFor("example.net"))
    }

    @Test
    fun `resetting detection preserves messages and reveals parent rules`() {
        val settings = settings()
        settings.saveServerConfig("example.net", listOf("parent"), listOf("Parent marker"))
        settings.saveServerConfig("play.example.net", listOf("child"), listOf("Child marker"))
        val editor = AutoGgConfigEditor(settings)
        editor.load("play.example.net")
        editor.toggleTriggers()
        assertFalse(editor.customTriggers)
        assertEquals("Parent marker", editor.triggers)
        editor.save("play.example.net")
        assertEquals(listOf("child"), settings.messagesFor("play.example.net"))
        assertEquals(listOf("Parent marker"), settings.triggersFor("play.example.net"))
    }

    @Test
    fun `unknown hosts stay inactive until custom triggers are explicitly chosen`() {
        val settings = settings()
        val editor = AutoGgConfigEditor(settings)
        editor.load("example.net")
        assertFalse(editor.customTriggers)
        assertEquals("", editor.triggers)
        editor.save("example.net")
        assertNull(settings.triggersFor("example.net"))
        editor.toggleTriggers()
        editor.save("example.net")
        assertEquals(emptyList(), settings.triggersFor("example.net"))
        editor.toggleTriggers()
        editor.save("example.net")
        assertNull(settings.triggersFor("example.net"))
    }

    @Test
    fun `editor previews builtin rules without saving them as overrides`() {
        val settings = settings()
        val editor = AutoGgConfigEditor(settings)
        editor.load("mc.hypixel.net")
        assertTrue(parseTriggers(editor.triggers).contains("Winner:"))
        assertTrue(parseTriggers(editor.triggers).contains(" won the game!"))
        editor.save("mc.hypixel.net")
        assertNull(settings.triggersFor("mc.hypixel.net"))
    }

    @Test
    fun `default editing and field resets preserve unrelated server settings`() {
        val settings = settings()
        settings.saveServerConfig("example.net", listOf("custom"), listOf("Fixture end"))
        val editor = AutoGgConfigEditor(settings)
        editor.messages = "new default"
        editor.save("")
        assertEquals("new default", settings.message)
        assertEquals(listOf("Fixture end"), settings.triggersFor("example.net"))
        assertFailsWith<IllegalArgumentException> { editor.toggleTriggers() }
        editor.load("example.net")
        editor.toggleMessages()
        editor.save("example.net")
        assertEquals(listOf("new default"), settings.messagesFor("example.net"))
        assertEquals(listOf("Fixture end"), settings.triggersFor("example.net"))
    }

    @Test
    fun `unsaved fields cannot be applied to a different or invalid hostname`() {
        val settings = settings()
        val editor = AutoGgConfigEditor(settings)
        editor.load("example.net")
        editor.toggleTriggers()
        editor.triggers = "Fixture end"
        assertFailsWith<IllegalArgumentException> { editor.save("another.net") }
        assertNull(settings.triggersFor("another.net"))
        assertFailsWith<IllegalArgumentException> { editor.load("invalid:25565") }
        assertEquals("example.net", editor.server)
        assertEquals("Fixture end", editor.triggers)
    }

    @Test
    fun `invalid trigger editor input cannot partially save valid messages`() {
        val settings = settings()
        settings.saveServerConfig("example.net", listOf("original"), listOf("Original marker"))
        val editor = AutoGgConfigEditor(settings)
        editor.load("example.net")
        editor.messages = "changed"
        editor.triggers = "valid||invalid"
        assertFailsWith<IllegalArgumentException> { editor.save("example.net") }
        assertEquals(listOf("original"), settings.messagesFor("example.net"))
        assertEquals(listOf("Original marker"), settings.triggersFor("example.net"))
    }

    @Test
    fun `trigger editor preserves literal whitespace pipes and backslashes`() {
        val values = listOf(" Winner: ", "fixture | winner", "fixture \\ result")
        assertEquals(values, parseTriggers(formatTriggers(values)))
        assertEquals(listOf("A", " B "), parseTriggers("A| B "))
        assertEquals(emptyList(), parseTriggers(""))
        assertFailsWith<IllegalArgumentException> { parseTriggers("A||B") }
        assertFailsWith<IllegalArgumentException> { parseTriggers("A\\") }
    }
}
