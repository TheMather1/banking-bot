package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.ButtonEmbedPaginator
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.*
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.editActionComponents
import java.util.concurrent.TimeUnit.MINUTES

@Service
class BankCommand(
    private val characterRepository: CharacterRepository,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "bank", "Access your bank accounts.") {

    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.user.idLong)
            val cancelButton = cancelButton(event.idLong)
            if (characters.isEmpty()) hook.editOriginal("You have no characters.")
                .setActionRow(cancelButton).queue {
                    eventWaiter.waitForCancelButton(cancelButton, event.user)
                }
            else {
                characterPaginator(characters, hook, event.user)
            }
        }
    }

    private fun accountMenu(message: Message, account: AccountEntity, user: User) {
        val accountEditButton = depositButton(message.idLong)
        val accountDeleteButton = withdrawButton(message.idLong)
        val cancelButton = cancelButton(message.idLong)
        message.editActionComponents(accountEditButton, accountDeleteButton, cancelButton).setContent(null).queue {
            eventWaiter.waitForButton(accountEditButton, user) {
                if(!deposit(it, account)) accountMenu(message, account, user)
            }
            eventWaiter.waitForButton(accountDeleteButton, user) {
                if(!withdraw(it, account)) accountMenu(message, account, user)
            }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    private fun characterPaginator(characters: List<CharacterEntity>, hook: InteractionHook, user: User) {
        lateinit var character: CharacterEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete()
                else accountPaginator(character.accounts, hook, user)
            }
            .setEventWaiter(eventWaiter)
            .setTimeout(5, MINUTES)
            .clearItems()
            .addItems(characters.map { it.asEmbed() })
            .setText { p, _ ->
                character = characters[p - 1]
                "Select character:"
            }.build().display(hook)
    }

    private fun accountPaginator(accounts: List<AccountEntity>, hook: InteractionHook, user: User) {
        lateinit var account: AccountEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete()
                else accountMenu(message, account, user)
            }
            .setEventWaiter(eventWaiter)
            .setTimeout(5, MINUTES)
            .clearItems()
            .addItems(accounts.map(AccountEntity::asEmbed))
            .setText { p, _ ->
                account = accounts[p - 1]
                "Select account:"
            }.build().display(hook)
    }

    private fun withdraw(event: ButtonInteractionEvent, account: AccountEntity): Boolean {
        val modal = withdrawModal(event.idLong)
        var success = false
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    when {
                        num < account.balance -> it.editOriginal("You cannot withdraw more than the account's balance.")
                        num > 0 -> {
                            account.withdraw(num)
                            characterRepository.saveAndFlush(account.character)
                            success = true
                            it.editOriginal("${account.character} withdrew $num from $account.").setComponents().setEmbeds()
                        }
                        else -> it.editOriginal("You must withdraw a positive amount.")
                    }
                } catch (_: NumberFormatException) {
                    it.editOriginal("$value is not a valid number.")
                }.queue()
            }
        }
        return success
    }

    private fun deposit(event: ButtonInteractionEvent, account: AccountEntity): Boolean {
        val modal = depositModal(event.idLong)
        var success = false
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    if (num > 0) {
                        account.deposit(num)
                        characterRepository.saveAndFlush(account.character)
                        success = true
                        it.editOriginal("${account.character} deposited $num to $account.").setComponents().setEmbeds()
                    } else it.editOriginal("You must deposit a positive amount.")
                } catch (_: NumberFormatException) {
                    it.editOriginal("$value is not a valid number.")
                }.queue()
            }
        }
        return success
    }

    companion object {
        private val goldField = TextInput.create("gold", "Enter value:", SHORT).setPlaceholder("0.00").setRequired(true).build()
        private fun depositButton(triggerId: Long) = ButtonImpl("edit_account_$triggerId", "Deposit", SUCCESS, false, null)
        private fun withdrawButton(triggerId: Long) = ButtonImpl("delete_account_$triggerId", "Withdraw", PRIMARY, false, null)
        private fun cancelButton(triggerId: Long) = ButtonImpl("cancel_$triggerId", "Cancel", DANGER, false, null)
        private fun depositModal(triggerId: Long) =
            Modal.create("deposit_${triggerId}", "Deposit").addActionRow(goldField).build()
        private fun withdrawModal(triggerId: Long) =
            Modal.create("withdraw_${triggerId}", "Withdraw").addActionRow(goldField).build()

    }
}