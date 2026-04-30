package cc.modlabs.autogg

import cc.modlabs.autogg.chat.EventDetector
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

class Autogg : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: Autogg
    }

    init {
        INSTANCE = this
    }

    lateinit var client: ClientWrapper
    lateinit var detector: EventDetector
    private var lastAutoGgAt: Long = 0L

    override fun onInitializeClient() {
        client = ClientWrapper()
        detector = EventDetector()

        ClientReceiveMessageEvents.GAME.register { message, _overlay ->
            val event = detector.scanForEvent(message.string) ?: return@register
            if (event == cc.modlabs.autogg.chat.Event.END_GAME) {
                val now = System.currentTimeMillis()
                if (now - lastAutoGgAt > 3_000) {
                    lastAutoGgAt = now
                    client.sendMessage("gg")
                }
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { _, _ ->
            if (client.getCurrentServer() == ServerConfig.MINEPLEX) {
                EventDetector.mineplexStart = false
            }
        })
    }
}
