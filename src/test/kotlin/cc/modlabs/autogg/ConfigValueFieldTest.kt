package cc.modlabs.autogg

import java.nio.file.Files
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigValueFieldTest {
    @Test
    fun `editing a long saved list does not truncate any trigger`() {
        val path = Files.createTempDirectory("autogg-large-editor").resolve("autogg.json")
        val original = List(10) { "trigger-$it-" + "x".repeat(990) }
        val settings = AutoGgSettings(path)
        settings.saveServerConfig("example.net", null, original)
        val editor = AutoGgConfigEditor(settings)
        editor.load("example.net")
        // A null font keeps this real Minecraft widget headless; no rendering is required.
        val constructor = EditBox::class.java.getConstructor(Font::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Component::class.java)
        val field = configureConfigValueField(constructor.newInstance(null, 300, 20, Component.literal("Test values")))
        field.value = editor.triggers
        assertEquals(editor.triggers, field.value)
        field.setResponder { editor.triggers = it }
        field.value = "T" + field.value.drop(1)
        editor.save("example.net")
        assertEquals(listOf("T" + original.first().drop(1)) + original.drop(1), AutoGgSettings(path).triggersFor("example.net"))
    }
}
