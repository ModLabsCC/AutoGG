package cc.modlabs.autogg

enum class ServerConfig(val ip: String) {
    HYPIXEL("hypixel.net"),
    BEDWARS_PRACTICE("bedwarspractice.club"),
    PVPLAND("pvp.land"),
    MINEMEN("minemen.club"),
    MINEPLEX("mineplex.com");

    fun matches(address: String): Boolean {
        val host = address.lowercase().substringBefore(':').trimEnd('.')
        return host == ip || host.endsWith(".$ip")
    }
}
