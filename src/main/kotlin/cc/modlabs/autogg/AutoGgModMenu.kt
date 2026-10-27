package cc.modlabs.autogg

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.io.IOException

class AutoGgModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent ->
        AutoGgConfigScreen(parent, Autogg.INSTANCE.settings)
    }
}

private class AutoGgConfigScreen(private val parent: Screen, private val settings: AutoGgSettings) :
    Screen(Component.literal("Auto GG settings")) {
    private lateinit var server: EditBox
    private lateinit var messages: EditBox
    private var status = ""

    override fun init() {
        val x = width / 2 - 150
        val currentServer = if (::server.isInitialized) server.value else ""
        val currentMessages = if (::messages.isInitialized) messages.value else formatMessages(settings.defaultMessages)
        server = addRenderableWidget(EditBox(font, x, 48, 300, 20, Component.literal("Server hostname, blank for default")))
        server.setMaxLength(253)
        server.setHint(Component.literal("Blank = default; e.g. hypixel.net"))
        server.value = currentServer
        messages = addRenderableWidget(EditBox(font, x, 92, 300, 20, Component.literal("Messages, separated by |")))
        messages.setMaxLength(8192)
        messages.value = currentMessages

        addRenderableWidget(Button.builder(Component.literal("Load")) { load() }.bounds(x, 122, 95, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Save")) { save() }.bounds(x + 102, 122, 95, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Remove override")) { remove() }.bounds(x + 204, 122, 96, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }.bounds(x + 100, 192, 100, 20).build())
    }

    private fun load() {
        val key = server.value.trim().lowercase().trimEnd('.')
        val values = if (key.isEmpty()) settings.defaultMessages else settings.overrides[key]
        messages.value = values?.let(::formatMessages) ?: ""
        status = if (values == null) "No override yet; enter messages and save." else "Loaded."
    }

    private fun save() {
        try {
            val values = parseMessages(messages.value)
            if (server.value.isBlank()) settings.saveDefaults(values) else settings.saveServer(server.value, values)
            status = "Saved."
        } catch (e: IllegalArgumentException) {
            status = e.message ?: "Invalid settings."
        } catch (e: IOException) {
            status = "Could not save settings: ${e.message}"
        }
    }

    private fun remove() {
        try {
            require(server.value.isNotBlank()) { "Enter a server to remove its override." }
            settings.saveServer(server.value, emptyList())
            messages.value = ""
            status = "Override removed; the default will be used."
        } catch (e: IllegalArgumentException) {
            status = e.message ?: "Invalid server."
        } catch (e: IOException) {
            status = "Could not save settings: ${e.message}"
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        val x = width / 2 - 150
        graphics.centeredText(font, title, width / 2, 20, -1)
        graphics.text(font, "Server hostname (blank for default)", x, 36, -1)
        graphics.text(font, "Messages: separate with | (escape as \\|)", x, 80, -1)
        graphics.textWithWordWrap(font, Component.literal(status), x, 148, 300, -1)
        graphics.text(font, "Overrides do not add server detection.", x, 176, -1)
    }

    override fun onClose() {
        minecraft.setScreenAndShow(parent)
    }
}

internal fun formatMessages(messages: List<String>): String =
    messages.joinToString(" | ") { it.replace("\\", "\\\\").replace("|", "\\|") }

internal fun parseMessages(text: String): List<String> {
    require(text.isNotBlank()) { "Enter at least one message." }
    val result = mutableListOf<String>()
    val item = StringBuilder()
    var escaping = false
    for (char in text) {
        when {
            escaping -> { require(char == '\\' || char == '|') { "Use \\\\ or \\| for literal characters." }; item.append(char); escaping = false }
            char == '\\' -> escaping = true
            char == '|' -> { result.add(item.toString().trim()); item.clear() }
            else -> item.append(char)
        }
    }
    require(!escaping) { "Incomplete escape at the end of the messages." }
    result.add(item.toString().trim())
    return result
}
