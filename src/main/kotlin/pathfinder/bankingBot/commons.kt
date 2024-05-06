package pathfinder.bankingBot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Denomination.GP
import kotlin.math.floor

fun Message.editActionComponents(vararg components: ItemComponent) = editMessageComponents(ActionRow.of(*components))

fun String.prepend(other: String) = other + this

fun truncateToCopper(value: Double, denomination: Denomination = GP): Double = floor(value * denomination(100.0)) / denomination(100.0)