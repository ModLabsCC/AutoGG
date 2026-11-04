package cc.modlabs.autogg

import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TriggerSettingsTest {
    @Test
    fun `trigger overrides survive reload independently from messages`() {
        val path = Files.createTempDirectory("autogg-triggers").resolve("autogg.json")
        val settings = AutoGgSettings(path)
        settings.saveServerConfig("EXAMPLE.NET.", listOf("well played"), listOf("Fixture end"))
        settings.saveServer("play.example.net", listOf("nice game"))
        val loaded = AutoGgSettings(path)
        assertEquals(listOf("Fixture end"), loaded.triggersFor("PLAY.EXAMPLE.NET:25565"))
        assertEquals(listOf("nice game"), loaded.messagesFor("play.example.net"))
        assertNull(loaded.triggersFor("notexample.net"))
        assertNull(loaded.triggersFor("example.net.other.org"))
        assertNull(loaded.triggersFor(null))
    }

    @Test
    fun `empty custom triggers disable while reset restores inherited rules`() {
        val path = Files.createTempDirectory("autogg-triggers").resolve("autogg.json")
        val settings = AutoGgSettings(path)
        settings.saveServerConfig("example.net", listOf("gg"), listOf("Fixture end"))
        settings.saveServerConfig("play.example.net", listOf("nice"), emptyList())
        assertEquals(emptyList(), AutoGgSettings(path).triggersFor("play.example.net"))
        settings.saveServerConfig("play.example.net", listOf("nice"), null)
        val loaded = AutoGgSettings(path)
        assertEquals(listOf("Fixture end"), loaded.triggersFor("play.example.net"))
        assertEquals(listOf("nice"), loaded.messagesFor("play.example.net"))
    }

    @Test
    fun `existing message schema and legacy import accept trigger saves`() {
        val dir = Files.createTempDirectory("autogg-legacy")
        val path = dir.resolve("autogg.json")
        Files.writeString(path, """{"default":["default gg"],"servers":{"example.net":["nice"]}}""")
        val old = AutoGgSettings(path)
        assertNull(old.triggersFor("example.net"))
        old.saveServerConfig("another.net", null, listOf("Fixture end"))
        assertEquals(listOf("nice"), AutoGgSettings(path).messagesFor("example.net"))
        val legacyPath = dir.resolve("legacy.json")
        Files.writeString(dir.resolve("autogg-message.txt"), "legacy gg")
        AutoGgSettings(legacyPath).saveServerConfig("example.net", null, listOf("Fixture end"))
        assertEquals("legacy gg", AutoGgSettings(legacyPath).message)
    }

    @Test
    fun `invalid server saves do not partially replace either field`() {
        val path = Files.createTempDirectory("autogg-validation").resolve("autogg.json")
        val settings = AutoGgSettings(path)
        settings.saveServerConfig("example.net", listOf("original"), listOf("Original marker"))
        val before = Files.readString(path)
        for (invalid in listOf("", " ", "a\nb", "a\u007fb", "§a", "a".repeat(1025))) {
            assertFailsWith<IllegalArgumentException> {
                settings.saveServerConfig("example.net", listOf("changed"), listOf(invalid))
            }
            assertEquals(before, Files.readString(path))
            assertEquals(listOf("original"), settings.messagesFor("example.net"))
            assertEquals(listOf("Original marker"), settings.triggersFor("example.net"))
        }
        assertFailsWith<IllegalArgumentException> {
            settings.saveServerConfig("example.net", listOf("/command"), listOf("Changed marker"))
        }
        assertEquals(before, Files.readString(path))
    }

    @Test
    fun `trigger whitespace and received command text remain literal`() {
        val path = Files.createTempDirectory("autogg-literal").resolve("autogg.json")
        AutoGgSettings(path).saveServerConfig("example.net", null, listOf(" Winner: ", "/match finished"))
        assertEquals(listOf(" Winner: ", "/match finished"), AutoGgSettings(path).triggersFor("example.net"))
    }

    @Test
    fun `malformed stored trigger data cannot be overwritten`() {
        val path = Files.createTempDirectory("autogg-corrupt").resolve("autogg.json")
        val bad = """{"default":["gg"],"servers":{},"triggers":{"example.net":[""]}}"""
        Files.writeString(path, bad)
        assertFailsWith<IOException> { AutoGgSettings(path).saveServerConfig("example.net", listOf("changed"), listOf("Fixture")) }
        assertEquals(bad, Files.readString(path))
    }
}
