package cc.modlabs.autogg

import net.minecraft.client.gui.components.EditBox

internal fun configureConfigValueField(field: EditBox): EditBox = field.apply {
    // Settings limit each item, not the whole list. Never truncate an existing valid list.
    setMaxLength(Int.MAX_VALUE)
}
