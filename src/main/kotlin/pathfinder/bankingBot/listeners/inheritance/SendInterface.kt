package pathfinder.bankingBot.listeners.inheritance

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository

interface SendInterface: InteractionTemplate {
    val eventWaiter: EventWaiter
    val characterRepository: CharacterRepository

    fun sendMenu(event: GenericCommandInteractionEvent, hook: InteractionHook, characters: List<CharacterEntity>, targetCharacters: List<CharacterEntity>) {
        val senderOptions = characters.map { SelectOption.of(it.name, it.id.toString()) }
        val recipientOptions = targetCharacters.map { SelectOption.of(it.name, it.id.toString()) }

        val senderAccountOptions = listOf(SelectOption.of("Account", "Account"))
        val recipientAccountOptions = listOf(SelectOption.of("Account", "Account"))

        handleInput(
            id = event.id,
            hook = hook,
            user = event.user,
            characters = characters,
            targetCharacters = targetCharacters,
            senderOptions = senderOptions,
            senderAccountOptions = senderAccountOptions,
            senderDisabled = true,
            recipientOptions = recipientOptions,
            recipientAccountOptions = recipientAccountOptions,
            recipientDisabled = true,
            sendButtonDisabled = true,
            sender = null,
            senderAccount = null,
            recipient = null,
            recipientAccount = null
        )
    }

    fun handleInput(
        id: String,
        hook: InteractionHook,
        user: User,
        characters: List<CharacterEntity>,
        targetCharacters: List<CharacterEntity>,
        senderOptions: List<SelectOption>,
        senderAccountOptions: List<SelectOption>,
        senderDisabled: Boolean,
        recipientOptions: List<SelectOption>,
        recipientAccountOptions: List<SelectOption>,
        recipientDisabled: Boolean,
        sendButtonDisabled: Boolean,
        sender: CharacterEntity?,
        senderAccount: AccountEntity?,
        recipient: CharacterEntity?,
        recipientAccount: AccountEntity?
    ) {
        val senderField = StringSelectMenu.create(id + "_sender_character")
            .addOptions(senderOptions)
            .setDefaultValues(listOfNotNull(sender?.id?.toString()))
            .setPlaceholder("Sender").setMaxValues(1).build()
        val recipientField = StringSelectMenu.create(id + "_recipient_character")
            .addOptions(recipientOptions)
            .setDefaultValues(listOfNotNull(recipient?.id?.toString()))
            .setPlaceholder("Recipient").setMaxValues(1).build()

        val senderAccounts = StringSelectMenu.create(id + "_sender_account")
            .addOptions(senderAccountOptions)
            .setDefaultValues(listOfNotNull(senderAccount?.id?.toString()))
            .setPlaceholder("Sender account").setMaxValues(1).setDisabled(senderDisabled).build()
        val recipientAccounts = StringSelectMenu.create(id + "_recipient_account")
            .addOptions(recipientAccountOptions)
            .setDefaultValues(listOfNotNull(recipientAccount?.id?.toString()))
            .setPlaceholder("Recipient account").setMaxValues(1).setDisabled(recipientDisabled).build()
        val sendButton = transactionSendButton(id).withDisabled(sendButtonDisabled)

        hook.editOriginalComponents(
            ActionRow.of(senderField),
            ActionRow.of(senderAccounts),
            ActionRow.of(recipientField),
            ActionRow.of(recipientAccounts),
            ActionRow.of(sendButton)
        ).setEmbeds(listOfNotNull(senderAccount?.asEmbed())).queue {
            eventWaiter.waitForSelection(senderField, user) { interaction ->
                interaction.deferEdit().queue()
                val newSender = characters.first { it.id.toString() == interaction.values.first() }
                val newSenderAccountOptions = newSender.accounts.map { SelectOption.of(it.toString(), it.id.toString()) }
                val senderChanged = newSender.id != sender?.id
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderOptions,
                    newSenderAccountOptions,
                    false,
                    recipientOptions,
                    recipientAccountOptions,
                    recipientDisabled,
                    sender = newSender,
                    senderAccount = senderAccount.takeUnless { senderChanged },
                    recipient = recipient,
                    recipientAccount = recipientAccount,
                    sendButtonDisabled = senderChanged || sendButtonDisabled
                )
            }
            eventWaiter.waitForSelection(senderAccounts, user) { interaction ->
                interaction.deferEdit().queue()
                val newSenderAccount = sender!!.accounts.first { it.id.toString() == interaction.values.first() }
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderOptions,
                    senderAccountOptions,
                    false,
                    recipientOptions,
                    recipientAccountOptions,
                    recipientDisabled,
                    sender = sender,
                    senderAccount = newSenderAccount,
                    recipient = recipient,
                    recipientAccount = recipientAccount,
                    sendButtonDisabled = recipientAccount == null
                )
            }
            eventWaiter.waitForSelection(recipientField, user) { interaction ->
                interaction.deferEdit().queue()
                val newRecipient = targetCharacters.first { it.id.toString() == interaction.values.first() }
                val newRecipientAccountOptions = newRecipient.accounts.map { SelectOption.of(it.toString(), it.id.toString()) }
                val recipientChanged = newRecipient.id != recipient?.id
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderOptions,
                    senderAccountOptions,
                    senderDisabled,
                    recipientOptions,
                    newRecipientAccountOptions,
                    false,
                    sender = sender,
                    senderAccount = senderAccount,
                    recipient = newRecipient,
                    recipientAccount = recipientAccount.takeUnless { recipientChanged },
                    sendButtonDisabled = recipientChanged || sendButtonDisabled
                )
            }
            eventWaiter.waitForSelection(recipientAccounts, user) { interaction ->
                interaction.deferEdit().queue()
                val newRecipientAccount = recipient!!.accounts.first { it.id.toString() == interaction.values.first() }
                handleInput(
                    interaction.id,
                    interaction.hook,
                    user,
                    characters,
                    targetCharacters,
                    senderOptions,
                    senderAccountOptions,
                    senderDisabled,
                    recipientOptions,
                    recipientAccountOptions,
                    false,
                    sender = sender,
                    senderAccount = senderAccount,
                    recipient = recipient,
                    recipientAccount = newRecipientAccount,
                    sendButtonDisabled = senderAccount == null
                )
            }
            eventWaiter.waitForButton(sendButton, user) { send(it, senderAccount!!, recipientAccount!!) }
        }
    }

    fun send(event: ButtonInteractionEvent, senderAccount: AccountEntity, recipientAccount: AccountEntity) {
        val modal = goldSendModal(event.idLong)
        event.replyModal(modal).queue {
            eventWaiter.waitForModal(modal, event) { modalEvent ->
                modalEvent.deferEdit().queue {
                    val input = modalEvent.getValue(goldField.id)!!.asString
                    try {
                        val gold = input.toDouble()
                        when {
                            senderAccount.id == recipientAccount.id -> it.editOriginal("Sending account cannot be the same as the recipient.")
                            gold <= 0 -> it.editOriginal("You must send a positive amount.")
                            gold > senderAccount.balance -> it.editOriginal("You cannot send more gold than you have.")
                            else -> {
                                val taxAccount = recipientAccount.accountType.taxConfig?.targetAccount
                                senderAccount.send(gold, recipientAccount)
                                recipientAccount.receive(gold, senderAccount)
                                characterRepository.saveAllAndFlush(listOfNotNull(senderAccount.character, recipientAccount.character, taxAccount?.character))
                                modalEvent.channel.sendMessage("${senderAccount.fullName()} sent $gold to ${recipientAccount.fullName()}").queue()
                                it.editOriginalComponents().setContent("Sent.")
                            }
                        }
                    } catch (_: NumberFormatException) {
                        it.editOriginal("$input is not a valid number.")
                    }.queue()
                }
            }
        }
    }

    companion object {
        private val goldField = TextInput.create("gold", "Enter amount to send:", SHORT).setPlaceholder("0.00").setRequired(true).build()
        private fun goldSendModal(triggerId: Long) =
            Modal.create("send_amount_${triggerId}", "Add character").addActionRow(goldField).build()
        private fun transactionSendButton(triggerId: String) = ButtonImpl("send_$triggerId", "Send", SUCCESS, false, null)

    }
}