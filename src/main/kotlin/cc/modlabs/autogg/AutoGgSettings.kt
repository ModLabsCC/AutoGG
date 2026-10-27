package cc.modlabs.autogg

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.slf4j.LoggerFactory

class AutoGgSettings(private val path: Path) {
    var defaultMessages: List<String> = listOf("gg")
        private set
    var overrides: Map<String, List<String>> = emptyMap()
        private set
    val message: String get() = defaultMessages.first()
    private var loadFailed = false

    init {
        try {
            if (Files.exists(path)) {
                val json = JsonParser.parseString(Files.readString(path)).asJsonObject
                val defaults = json.getAsJsonArray("default").map { it.asString }
                val servers = json.getAsJsonObject("servers").entrySet().associate { entry ->
                    normalizeServer(entry.key) to entry.value.asJsonArray.map { it.asString }
                }
                validateMessages(defaults)
                servers.values.forEach(::validateMessages)
                defaultMessages = defaults
                overrides = servers
            } else {
                val legacy = path.resolveSibling("autogg-message.txt")
                if (Files.exists(legacy)) {
                    val old = Files.readString(legacy).trim()
                    validateMessages(listOf(old))
                    defaultMessages = listOf(old)
                }
            }
        } catch (e: Exception) {
            loadFailed = true
            LoggerFactory.getLogger("AutoGG").warn("Could not load AutoGG settings", e)
        }
    }

    fun messagesFor(address: String?): List<String> {
        val host = address?.lowercase()?.substringBefore(':')?.trimEnd('.') ?: return defaultMessages
        return overrides.entries.filter { (server, _) -> host == server || host.endsWith(".$server") }
            .maxByOrNull { it.key.length }?.value ?: defaultMessages
    }

    fun save(value: String) = saveDefaults(listOf(value))

    fun saveDefaults(messages: List<String>) {
        val cleaned = messages.map(String::trim)
        validateMessages(cleaned)
        write(cleaned, overrides)
        defaultMessages = cleaned
    }

    fun saveServer(server: String, messages: List<String>) {
        val name = normalizeServer(server)
        val cleaned = messages.map(String::trim)
        if (cleaned.isNotEmpty()) validateMessages(cleaned)
        val updated = overrides.toMutableMap()
        if (cleaned.isEmpty()) updated.remove(name) else updated[name] = cleaned
        write(defaultMessages, updated)
        overrides = updated
    }

    private fun write(defaults: List<String>, servers: Map<String, List<String>>) {
        if (loadFailed) throw IOException("Existing AutoGG settings could not be read; they were not overwritten.")
        val json = JsonObject()
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        json.add("default", gson.toJsonTree(defaults))
        json.add("servers", gson.toJsonTree(servers))
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "autogg-", ".tmp")
        try {
            Files.writeString(temporary, gson.toJson(json))
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun validateMessages(messages: List<String>) {
        require(messages.isNotEmpty() && messages.all { value ->
            value.isNotBlank() && value.length <= 256 && !value.startsWith('/') && value.none { it < ' ' || it == '\u007f' || it == '\u00a7' }
        }) { "Each message must be 1-256 characters, without commands or control characters." }
    }

    private fun normalizeServer(server: String): String {
        val normalized = server.trim().lowercase().trimEnd('.')
        require(normalized.length <= 253 && normalized.split('.').all { it.matches(Regex("[a-z0-9]+(?:[a-z0-9-]*[a-z0-9])?")) }) {
            "Enter a server hostname without a port."
        }
        return normalized
    }
}
