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

private class AutoGgConfigScreen(private val parent: Screen, settings: AutoGgSettings) :
    Screen(Component.literal("Auto GG settings")) {
    private val editor = AutoGgConfigEditor(settings)
    private lateinit var server: EditBox
    private lateinit var values: EditBox
    private lateinit var mode: Button
    private lateinit var messagesTab: Button
    private lateinit var triggersTab: Button
    private var showTriggers = false
    private var refreshing = false
    private var status = "Enter a hostname and Load, or edit the default messages below."

    override fun init() {
        val x = width / 2 - 150
        val hostname = if (::server.isInitialized) server.value else editor.server
        server = addRenderableWidget(EditBox(font, x, 42, 198, 20, Component.literal("Server hostname, blank for default")))
        server.setMaxLength(253)
        server.setHint(Component.literal("Blank = default messages"))
        server.value = hostname
        server.setResponder { status = "Select Load before editing a different hostname." }
        addRenderableWidget(Button.builder(Component.literal("Load")) {
            attempt {
                editor.load(server.value)
                refreshFields()
                status = if (showTriggers) editor.detectionDescription() else "Loaded. Save to apply your changes."
            }
        }.bounds(x + 204, 42, 96, 20).build())

        messagesTab = addRenderableWidget(Button.builder(Component.literal("Messages")) {
            showTriggers = false
            refreshFields()
            status = "One message is chosen at random. Use \\| for a literal pipe."
        }.bounds(x, 68, 147, 20).build())
        triggersTab = addRenderableWidget(Button.builder(Component.literal("End triggers")) {
            showTriggers = true
            refreshFields()
            status = editor.detectionDescription()
        }.bounds(x + 153, 68, 147, 20).build())

        values = addRenderableWidget(EditBox(font, x, 106, 300, 20, Component.literal("Configured values, separated by pipes")))
        values.setMaxLength(8192)
        values.setResponder { value ->
            if (!refreshing) {
                if (showTriggers) editor.triggers = value else editor.messages = value
                status = if (showTriggers && value.isEmpty()) "Empty custom triggers disable detection. Save to apply." else "Unsaved changes. Select Save to apply."
            }
        }
        mode = addRenderableWidget(Button.builder(Component.literal("Customize")) {
            attempt {
                if (showTriggers) editor.toggleTriggers() else editor.toggleMessages()
                refreshFields()
                status = if (showTriggers) editor.detectionDescription() else "Message inheritance changed. Save to apply."
            }
        }.bounds(x, 132, 300, 20).build())

        addRenderableWidget(Button.builder(Component.literal("Save")) {
            attempt {
                editor.save(server.value)
                status = "Saved ${if (editor.isDefault) "default messages" else "settings for ${editor.server}"}."
            }
        }.bounds(x, height - 30, 147, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
            .bounds(x + 153, height - 30, 147, 20).build())
        refreshFields()
    }

    private fun refreshFields() {
        refreshing = true
        values.value = if (showTriggers) editor.triggers else editor.messages
        values.setEditable(if (showTriggers) editor.customTriggers else editor.customMessages)
        mode.active = !editor.isDefault
        mode.message = Component.literal(when {
            editor.isDefault -> "Load a hostname for per-server overrides"
            showTriggers && editor.customTriggers -> "Reset to inherited detection"
            showTriggers -> "Customize detection"
            editor.customMessages -> "Reset to inherited messages"
            else -> "Customize messages"
        })
        messagesTab.message = Component.literal(if (showTriggers) "Messages" else "[ Messages ]")
        triggersTab.message = Component.literal(if (showTriggers) "[ End triggers ]" else "End triggers")
        refreshing = false
    }

    private fun attempt(action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalArgumentException) {
            status = e.message ?: "Invalid settings."
        } catch (e: IOException) {
            status = "Could not save: ${e.message}"
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        val x = width / 2 - 150
        graphics.centeredText(font, title, width / 2, 12, -1)
        graphics.text(font, "Server hostname (blank for default messages)", x, 30, -1)
        graphics.text(font, if (showTriggers) "Triggers: | separated; case and spaces matter" else "Messages: separate with | (escape as \\|)", x, 94, -1)
        graphics.textWithWordWrap(font, Component.literal(status.take(140)), x, 160, 300, -1)
    }

    override fun onClose() {
        minecraft.setScreenAndShow(parent)
    }
}
