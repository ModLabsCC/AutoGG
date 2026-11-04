package cc.modlabs.autogg

import cc.modlabs.autogg.chat.EventDetector

/** Unsaved editor state, separate from Minecraft widgets so scope and save behavior can be tested. */
internal class AutoGgConfigEditor(private val settings: AutoGgSettings) {
    private val detector = EventDetector()
    var server = ""
        private set
    var messages = formatMessages(settings.defaultMessages)
    var triggers = ""
    var customMessages = true
        private set
    var customTriggers = false
        private set
    val isDefault: Boolean get() = server.isEmpty()

    fun load(hostname: String) {
        val name = key(hostname)
        server = name
        customMessages = isDefault || settings.overrides.containsKey(name)
        customTriggers = !isDefault && settings.triggerOverrides.containsKey(name)
        messages = formatMessages(settings.messagesFor(name))
        triggers = if (isDefault) "" else formatTriggers(settings.triggersFor(name) ?: builtInTriggers())
    }

    fun toggleMessages() {
        require(!isDefault) { "Default messages cannot inherit from another setting." }
        customMessages = !customMessages
        if (!customMessages) messages = formatMessages(
            matchingOverride(server, settings.overrides - server) ?: settings.defaultMessages
        )
    }

    fun toggleTriggers() {
        require(!isDefault) { "Enter and load a server hostname to configure detection." }
        customTriggers = !customTriggers
        if (!customTriggers) triggers = formatTriggers(inheritedTriggers() ?: builtInTriggers())
    }

    fun save(hostname: String) {
        require(key(hostname) == server) { "Load the new hostname before editing or saving its settings." }
        if (isDefault) {
            settings.saveDefaults(parseMessages(messages))
        } else {
            settings.saveServerConfig(server,
                if (customMessages) parseMessages(messages) else null,
                if (customTriggers) parseTriggers(triggers) else null)
        }
    }

    fun detectionDescription(): String = when {
        isDefault -> "Detection is configured per server. Enter a hostname and select Load."
        customTriggers && triggers.isEmpty() -> "Custom detection is disabled: an empty list matches nothing."
        customTriggers -> "Custom triggers replace built-in rules. Save to apply."
        inheritedTriggers() != null -> "Using parent-domain triggers. Customize to override them."
        builtInTriggers().isNotEmpty() -> "Using built-in detection. Customize to replace it."
        else -> "No detection configured. Customize and enter real server end text."
    }

    private fun inheritedTriggers(): List<String>? = matchingOverride(server, settings.triggerOverrides - server)
    private fun builtInTriggers(): List<String> = detector.builtInTriggers(ServerConfig.entries.firstOrNull { it.matches(server) })
    private fun key(hostname: String): String = if (hostname.isBlank()) "" else normalizeServer(hostname)
}
