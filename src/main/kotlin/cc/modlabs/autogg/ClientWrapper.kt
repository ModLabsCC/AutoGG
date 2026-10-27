package cc.modlabs.autogg

import net.minecraft.client.Minecraft

class ClientWrapper {
    private val client: Minecraft = Minecraft.getInstance()
    val serverAddress: String? get() = client.currentServer?.ip

    fun getCurrentServer(): ServerConfig? {
        val address = serverAddress ?: return null

        for (server in ServerConfig.entries) {
            if (server.matches(address)) return server
        }
        return null
    }

    fun sendMessage(messageToSend: String): Boolean {
        val connection = client.player?.connection ?: return false
        connection.sendChat(messageToSend)
        return true
    }
}
