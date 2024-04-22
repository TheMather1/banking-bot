package pathfinder.bankingBot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.OptionType.*
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import pathfinder.bankingBot.banking.jpa.Paginatable


const val GOLD_FIELD = "gold"
const val NAME_FIELD = "name"
const val PLAYER_OPTION = "player"

val typeOption = OptionData(STRING, "type", "Account type.", true)
val interestOption = OptionData(
    STRING, "interest-rate", "Monthly interest rate of the account as a percentage. Accepts dice syntax.", true
)
val playerOption = OptionData(USER, PLAYER_OPTION, "Owner of the character.", true)
val channelOption = OptionData(CHANNEL, "channel", "The channel to use. (None to disable)", false)
val booleanOption = OptionData(BOOLEAN, "bool", "TRUE/FALSE", true)

internal const val INVALID_COMMAND = "Invalid command"

val goldRegex = "^\\d+\\.?\\d{0,2}\$".toRegex()

//internal fun <R> HTreeMap<Long, Bank>.onBank(id: Long, op: Bank.() -> R): R {
//    val bank = this[id] ?: Bank(id)
//    return op(bank).also {
//        this[id] = bank
//    }
//}
//
//fun <R> IReplyCallback.onBank(registrations: HTreeMap<Long, Bank>, op: Bank.() -> R): R {
//    val activeJda = jda
//    return registrations.onBank(guild!!.idLong) {
//        jda = activeJda
//        op()
//    }
//}

fun <E> Collection<E>.nonEmpty() = takeUnless { it.isEmpty() }

fun IMessageEditCallback.deferReply(op: MessageEditCallbackAction.() -> Unit) {
    op(deferEdit())
}
fun IMessageEditCallback.deferEdit(op: MessageEditCallbackAction.() -> Unit) = deferEdit().also(op)

fun IReplyCallback.deferReply(op: ReplyCallbackAction.() -> Unit) = deferReply().also(op)

fun List<Paginatable>.asEmbeds() = map { it.asEmbed() }

fun Message.editActionComponents(vararg components: ItemComponent) = editMessageComponents(ActionRow.of(*components))

fun String.prepend(other: String) = other + this