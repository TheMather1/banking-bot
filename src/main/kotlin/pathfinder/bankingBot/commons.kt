package pathfinder.bankingBot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Denomination.GP
import kotlin.math.floor

fun Message.editActionComponents(vararg components: ItemComponent) = editMessageComponents(ActionRow.of(*components))

fun String.prepend(other: String) = other + this

fun String.delim(maxLength: Int = 45) = if (length > maxLength) substring(0, maxLength-3) + "..." else this

fun truncateToCopper(value: Double, denomination: Denomination = GP): Double = floor(value * denomination(100.0)) / denomination(100.0)


const val NUMBER_FORMAT = "%,.2f"