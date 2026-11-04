package cc.modlabs.autogg

internal fun formatMessages(messages: List<String>): String =
    messages.joinToString(" | ", transform = ::escapeItem)

internal fun formatTriggers(triggers: List<String>): String =
    triggers.joinToString("|", transform = ::escapeItem)

private fun escapeItem(value: String): String = value.replace("\\", "\\\\").replace("|", "\\|")

internal fun parseMessages(text: String): List<String> = parseItems(text, trim = true)

internal fun parseTriggers(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    return parseItems(text, trim = false).also { values ->
        require(values.none(String::isBlank)) { "Triggers cannot contain empty entries. Clear the entire list to disable detection." }
    }
}

private fun parseItems(text: String, trim: Boolean): List<String> {
    require(text.isNotBlank()) { "Enter at least one message or trigger." }
    val result = mutableListOf<String>()
    val item = StringBuilder()
    var escaping = false
    fun finish() {
        result.add(if (trim) item.toString().trim() else item.toString())
        item.clear()
    }
    for (char in text) {
        when {
            escaping -> { require(char == '\\' || char == '|') { "Only pipes and backslashes can be escaped." }; item.append(char); escaping = false }
            char == '\\' -> escaping = true
            char == '|' -> finish()
            else -> item.append(char)
        }
    }
    require(!escaping) { "Incomplete escape at the end of the list." }
    finish()
    return result
}
