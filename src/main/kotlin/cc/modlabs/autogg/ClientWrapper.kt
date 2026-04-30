package cc.modlabs.autogg

import net.minecraft.client.Minecraft

class ClientWrapper {
    private val client: Minecraft = Minecraft.getInstance()

    fun getCurrentServer(): ServerConfig? {
        val address = client.currentServer?.ip ?: return null

        for (server in ServerConfig.entries) {
            if (address.contains(server.ip)) return server
        }
        return null
    }

    fun sendMessage(messageToSend: String?) {
        if (messageToSend.isNullOrBlank()) return
        client.player?.connection?.sendChat(messageToSend)
    }
}