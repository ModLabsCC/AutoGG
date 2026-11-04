package cc.modlabs.autogg.chat

import cc.modlabs.autogg.ServerConfig

class EventDetector {
    private val serverMessageEvents: MutableMap<ServerConfig, MutableMap<String, Event>> = mutableMapOf()

    init {
        initTable()
    }

    private fun initTable() {
        // END STRINGS
        val hypixel = mutableMapOf<String, Event>()

        hypixel["Your Overall Winstreak:"] = Event.END_GAME
        hypixel["1st Place -"] = Event.END_GAME
        hypixel["1st Killer -"] = Event.END_GAME
        hypixel[" - Damage Dealt -"] = Event.END_GAME
        hypixel["Winning Team -"] = Event.END_GAME
        hypixel["1st -"] = Event.END_GAME
        hypixel["Winners:"] = Event.END_GAME
        hypixel["Winner:"] = Event.END_GAME
        hypixel["Winning Team:"] = Event.END_GAME
        hypixel[" won the game!"] = Event.END_GAME
        hypixel["Top Seeker:"] = Event.END_GAME
        hypixel["1st Place:"] = Event.END_GAME
        hypixel["Last team standing!"] = Event.END_GAME
        hypixel["Winner #1 ("] = Event.END_GAME
        hypixel["Top Survivors"] = Event.END_GAME
        hypixel["Winners -"] = Event.END_GAME
        hypixel["Sumo Duel -"] = Event.END_GAME
        hypixel["Most Wool Placed -"] = Event.END_GAME
        hypixel["This game has been recorded."] = Event.END_GAME

        serverMessageEvents[ServerConfig.HYPIXEL] = hypixel


        val bedwarsPractice = mutableMapOf<String, Event>()

        bedwarsPractice["Winners -"] = Event.END_GAME
        bedwarsPractice["Game Won!"] = Event.END_GAME
        bedwarsPractice["Game Lost!"] = Event.END_GAME
        bedwarsPractice["The winning team is"] = Event.END_GAME

        serverMessageEvents[ServerConfig.BEDWARS_PRACTICE] = bedwarsPractice


        val pvpland = mutableMapOf<String, Event>()

        pvpland["The match has ended!"] = Event.END_GAME
        pvpland["Match Results"] = Event.END_GAME
        pvpland["Winner:"] = Event.END_GAME
        pvpland["Loser:"] = Event.END_GAME

        serverMessageEvents[ServerConfig.PVPLAND] = pvpland

        val minemen = mutableMapOf<String, Event>()

        minemen["Match Results"] = Event.END_GAME

        serverMessageEvents[ServerConfig.MINEMEN] = minemen

        val mineplex = mutableMapOf<String, Event>()

        mineplex["Chat> Chat is no longer silenced."] = Event.END_GAME

        serverMessageEvents[ServerConfig.MINEPLEX] = mineplex
    }

    fun builtInTriggers(server: ServerConfig?): List<String> =
        serverMessageEvents[server]?.keys?.toList() ?: emptyList()

    fun scanForEvent(message: String, server: ServerConfig?, customTriggers: List<String>? = null): Event? {
        if (customTriggers != null) {
            return if (customTriggers.any(message::contains)) Event.END_GAME else null
        }
        val serverConfig = server ?: return null

        if (serverConfig === ServerConfig.MINEPLEX) {
            if (message.contains("You have been sent from ") && message.contains(" to Lobby")) {
                mineplexStart = false
            } else if (message.contains("1st Place")) {
                mineplexStart = true
            }
        }

        for (s in serverMessageEvents[serverConfig]?.keys ?: return null) {
            if (message.contains(s)) {
                val event = serverMessageEvents[serverConfig]?.get(s) ?: return null

                if (serverConfig === ServerConfig.MINEPLEX && event == Event.END_GAME) {
                    mineplexStart = !mineplexStart
                    return if (mineplexStart) Event.START_GAME else Event.END_GAME
                }

                return when (event) {
                    Event.END_GAME -> event
                    else -> null
                }
            }
        }
        return null
    }

    private var mineplexStart = false

    fun reset() {
        mineplexStart = false
    }
}