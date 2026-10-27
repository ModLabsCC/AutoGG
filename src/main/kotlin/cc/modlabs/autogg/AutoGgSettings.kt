package cc.modlabs.autogg

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.slf4j.LoggerFactory

class AutoGgSettings(private val path: Path) {
    var message: String = "gg"
        private set

    init {
        if (Files.exists(path)) {
            try {
                val saved = Files.readString(path)
                requireValid(saved)
                message = saved
            } catch (e: Exception) {
                LoggerFactory.getLogger("AutoGG").warn("Could not load AutoGG message from $path", e)
            }
        }
    }

    fun save(value: String) {
        val text = value.trim()
        requireValid(text)
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "autogg-", ".tmp")
        try {
            Files.writeString(temporary, text)
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
        message = text
    }

    private fun requireValid(value: String) {
        require(value.isNotBlank() && value.length <= 256 && !value.startsWith('/') && value.none { it < ' ' || it == '\u007f' || it == '\u00a7' }) {
            "Use a chat message of 1-256 characters, without commands or control characters."
        }
    }
}
