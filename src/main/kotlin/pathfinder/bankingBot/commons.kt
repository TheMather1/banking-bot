package pathfinder.bankingBot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent

fun Message.editActionComponents(vararg components: ItemComponent) = editMessageComponents(ActionRow.of(*components))

fun String.prepend(other: String) = other + this

fun truncateToCopper(value: Double): Double = Math.round(value * 100) /100.0