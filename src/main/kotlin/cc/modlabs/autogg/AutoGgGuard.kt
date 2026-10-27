package cc.modlabs.autogg

class AutoGgGuard {
    private var lastEventAt: Long? = null
    private var lastChat: String? = null

    fun shouldSend(server: ServerConfig?, message: String, now: Long): Boolean {
        val previous = lastEventAt
        lastEventAt = now
        if (previous != null && now - previous < 3_000) return false
        return server != ServerConfig.HYPIXEL || lastChat != message
    }

    fun sent(message: String) {
        lastChat = message
    }

    fun reset() {
        lastEventAt = null
        lastChat = null
    }
}
