package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.command.UserContextMenuEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.deferEdit

@Service
class SendMenu(
    private val characterRepository: CharacterRepository,
    private val eventWaiter: EventWaiter
) : InteractionTemplate, UserContextMenu() {

    init {
        name = "Send money"
    }

    override fun execute(event: UserContextMenuEvent) {
        event.deferReply(true).queue { hook ->
            val target = event.targetMember!!
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.user.idLong)
            val targetCharacters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, target.user.idLong)

            val senderField = StringSelectMenu.create(event.id + "_sender_character")
                .addOptions(characters.map { SelectOption.of(it.name, it.id.toString()) })
                .setPlaceholder("Sender").setMaxValues(1).build()
            val recipientField = StringSelectMenu.create(event.id + "_recipient_character")
                .addOptions(targetCharacters.map { SelectOption.of(it.name, it.id.toString()) })
                .setPlaceholder("Recipient").setMaxValues(1).build()

            val senderAccounts = StringSelectMenu.create(event.id + "_sender_account")
                .addOptions(SelectOption.of("Account", "Account"))
                .setPlaceholder("Sender account").setMaxValues(1).setDisabled(true).build()
            val recipientAccounts = StringSelectMenu.create(event.id + "_recipient_account")
                .addOptions(SelectOption.of("Account", "Account"))
                .setPlaceholder("Recipient account").setMaxValues(1).setDisabled(true).build()

            val sendButton = transactionSendButton(event.id).asDisabled()
            handleInput(
                event.id,
                hook,
                event.user,
                characters,
                targetCharacters,
                senderField,
                senderAccounts,
                recipientField,
                recipientAccounts,
                sendButton,
                null,
                null,
                null,
                null
            )
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun handleInput(
        id: String,
        hook: InteractionHook,
        user: User,
        characters: List<CharacterEntity>,
        targetCharacters: List<CharacterEntity>,
        senderField: StringSelectMenu,
        senderAccounts: StringSelectMenu,
        recipientField: StringSelectMenu,
        recipientAccounts: StringSelectMenu,
        sendButton: Button,
        sender: CharacterEntity?,
        senderAccount: AccountEntity?,
        recipient: CharacterEntity?,
        recipientAccount: AccountEntity?
    ) {
        val senderField = StringSelectMenu.create(id + "_sender_character")
            .addOptions(senderField.options)
            .setDefaultValues(listOfNotNull(sender?.id?.toString()))
            .setPlaceholder("Sender").setMaxValues(1).build()
        val recipientField = StringSelectMenu.create(id + "_recipient_character")
            .addOptions(recipientField.options)
            .setDefaultValues(listOfNotNull(recipient?.id?.toString()))
            .setPlaceholder("Recipient").setMaxValues(1).build()

        val senderAccounts = StringSelectMenu.create(id + "_sender_account")
            .addOptions(senderAccounts.options)
            .setDefaultValues(listOfNotNull(senderAccount?.id?.toString()))
            .setPlaceholder("Sender account").setMaxValues(1).setDisabled(senderAccounts.isDisabled).build()
        val recipientAccounts = StringSelectMenu.create(id + "_recipient_account")
            .addOptions(recipientAccounts.options)
            .setDefaultValues(listOfNotNull(recipientAccount?.id?.toString()))
            .setPlaceholder("Recipient account").setMaxValues(1).setDisabled(recipientAccounts.isDisabled).build()
        val sendButton = transactionSendButton(id).withDisabled(sendButton.isDisabled)

        hook.editOriginalComponents(
            ActionRow.of(senderField),
            ActionRow.of(senderAccounts),
            ActionRow.of(recipientField),
            ActionRow.of(recipientAccounts),
            ActionRow.of(sendButton)
        ).queue {
            eventWaiter.waitForSelection(senderField, user) { interaction ->
                interaction.deferEdit().queue()
                val newSender = characters.first { it.id.toString() == interaction.values.first() }
                val newSenderField = StringSelectMenu.create(senderField.id!!).addOptions(senderField.options)
                    .setDefaultValues(newSender.id.toString()).build()
                val newSenderAccounts = StringSelectMenu.create(senderAccounts.id!!)
                    .addOptions(newSender.accounts.map { SelectOption.of(it.toString(), it.id.toString()) }).build()
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    newSenderField,
                    newSenderAccounts,
                    recipientField,
                    recipientAccounts,
                    sender = newSender,
                    senderAccount = null,
                    recipient = recipient,
                    recipientAccount = recipientAccount,
                    sendButton = sendButton.asDisabled()
                )
            }
            eventWaiter.waitForSelection(senderAccounts, user) { interaction ->
                interaction.deferEdit().queue()
                val newSenderAccount = sender!!.accounts.first { it.id.toString() == interaction.values.first() }
                val newSenderAccounts = StringSelectMenu.create(senderAccounts.id!!).addOptions(senderAccounts.options)
                    .setDefaultValues(newSenderAccount.id.toString()).build()
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderField,
                    newSenderAccounts,
                    recipientField,
                    recipientAccounts,
                    sender = sender,
                    senderAccount = newSenderAccount,
                    recipient = recipient,
                    recipientAccount = recipientAccount,
                    sendButton = if (recipientAccount != null) sendButton.asEnabled() else sendButton.asDisabled()
                )
            }
            eventWaiter.waitForSelection(recipientField, user) { interaction ->
                interaction.deferEdit().queue()
                val newRecipient = targetCharacters.first { it.id.toString() == interaction.values.first() }
                val newRecipientField = StringSelectMenu.create(recipientField.id!!).addOptions(recipientField.options)
                    .setDefaultValues(newRecipient.id.toString()).build()
                val newRecipientAccounts = StringSelectMenu.create(recipientAccounts.id!!)
                    .addOptions(newRecipient.accounts.map { SelectOption.of(it.toString(), it.id.toString()) }).build()
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderField,
                    senderAccounts,
                    newRecipientField,
                    newRecipientAccounts,
                    sender = sender,
                    senderAccount = senderAccount,
                    recipient = newRecipient,
                    recipientAccount = null,
                    sendButton = sendButton.asDisabled()
                )
            }
            eventWaiter.waitForSelection(recipientAccounts, user) { interaction ->
                interaction.deferEdit().queue()
                val newRecipientAccount = recipient!!.accounts.first { it.id.toString() == interaction.values.first() }
                val newRecipientAccounts =
                    StringSelectMenu.create(recipientAccounts.id!!).addOptions(recipientAccounts.options)
                        .setDefaultValues(newRecipientAccount.id.toString()).build()
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderField,
                    senderAccounts,
                    recipientField,
                    newRecipientAccounts,
                    sender = sender,
                    senderAccount = senderAccount,
                    recipient = recipient,
                    recipientAccount = newRecipientAccount,
                    sendButton = if (senderAccount != null) sendButton.asEnabled() else sendButton.asDisabled()
                )
            }
            eventWaiter.waitForButton(sendButton, user) { send(it, senderAccount!!, recipientAccount!!) }
        }
    }

    private fun send(event: ButtonInteractionEvent, sender: AccountEntity, recipient: AccountEntity) {
        val modal = goldSendModal(event.idLong)
        event.replyModal(modal).queue {
            eventWaiter.waitForModal(modal, event) { modalEvent ->
                modalEvent.deferEdit {
                    val input = modalEvent.getValue(goldField.id)!!.asString
                    try {
                        val gold = input.toDouble()
                        when {
                            sender.id == recipient.id -> setContent("Sending account cannot be the same as the recipient.")
                            gold <= 0 -> setContent("You must send a positive amount.")
                            gold > sender.balance -> setContent("You cannot send more gold than you have.")
                            else -> {
                                sender.send(gold, recipient)
                                recipient.receive(gold, sender)
                                characterRepository.saveAllAndFlush(listOf(sender.character, recipient.character))
                                modalEvent.channel.sendMessage ("${sender.fullName()} sent $gold to ${recipient.fullName()}").queue()
                                setComponents().setContent("Sent.")
                            }
                        }
                    } catch (_: NumberFormatException) {
                        setContent("$input is not a valid number.")
                    }.queue()
                }
            }
        }
    }

    companion object {
        private val goldField = TextInput.create("gold", "Enter amount to send:", SHORT).setPlaceholder("0.00").setRequired(true).build()
        private fun transactionSendButton(triggerId: String) = ButtonImpl("send_$triggerId", "Send", SUCCESS, false, null)
        private fun goldSendModal(triggerId: Long) =
            Modal.create("send_amount_${triggerId}", "Add character").addActionRow(goldField).build()
    }
}