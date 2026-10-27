package cc.modlabs.autogg

import cc.modlabs.autogg.chat.EventDetector
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import java.io.IOException

class Autogg : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: Autogg
    }

    init {
        INSTANCE = this
    }

    lateinit var client: ClientWrapper
    lateinit var detector: EventDetector
    lateinit var settings: AutoGgSettings
    private val guard = AutoGgGuard()

    override fun onInitializeClient() {
        client = ClientWrapper()
        detector = EventDetector()
        settings = AutoGgSettings(FabricLoader.getInstance().configDir.resolve("autogg.json"))

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("autogg")
                .executes { context ->
                    context.source.sendFeedback(Component.literal("AutoGG message: ${settings.message}. Change it with /autogg <message>"))
                    1
                }
                .then(argument("message", StringArgumentType.greedyString()).executes { context ->
                    try {
                        settings.save(StringArgumentType.getString(context, "message"))
                        context.source.sendFeedback(Component.literal("AutoGG message set to: ${settings.message}"))
                        1
                    } catch (e: IllegalArgumentException) {
                        context.source.sendError(Component.literal(e.message ?: "Invalid message."))
                        0
                    } catch (e: IOException) {
                        context.source.sendError(Component.literal("Could not save the AutoGG message: ${e.message}"))
                        0
                    }
                }))
        }

        ClientSendMessageEvents.CHAT.register { message -> guard.sent(message) }

        ClientReceiveMessageEvents.GAME.register { message, _overlay ->
            val event = detector.scanForEvent(message.string) ?: return@register
            if (event == cc.modlabs.autogg.chat.Event.END_GAME) {
                val server = client.getCurrentServer()
                val messages = settings.messagesFor(client.serverAddress)
                val selected = guard.chooseMessage(server, messages)
                if (guard.shouldSend(server, selected ?: messages.first(), System.nanoTime() / 1_000_000) && selected != null) {
                    if (client.sendMessage(selected)) guard.sent(selected)
                }
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { _, _ ->
            guard.reset()
            if (client.getCurrentServer() == ServerConfig.MINEPLEX) {
                EventDetector.mineplexStart = false
            }
        })
    }
}
