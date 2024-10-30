package pathfinder.bankingBot.service.support

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.ButtonEmbedPaginator
import jakarta.transaction.Transactional
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.*
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.repository.AccountEntityRepository
import pathfinder.bankingBot.banking.jpa.repository.AccountTypeRepository
import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository
import pathfinder.bankingBot.banking.jpa.service.BankService
import pathfinder.bankingBot.delim
import pathfinder.bankingBot.editActionComponents
import pathfinder.bankingBot.listeners.inheritance.InteractionTemplate
import pathfinder.bankingBot.listeners.input.AccountTypeField
import pathfinder.bankingBot.listeners.input.DowntimeFields
import pathfinder.bankingBot.prepend
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.concurrent.thread

@Service
class CharacterSupport(
    private val eventWaiter: EventWaiter,
    private val characterRepository: CharacterRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val accountEntityRepository: AccountEntityRepository,
    private val downtimeSupport: DowntimeSupport,
    private val bankService: BankService,
) : InteractionTemplate {

    @Transactional
    fun characterMenu(message: Message, character: CharacterEntity, user: User) {
        val accountAddButton = accountAddButton(message.idLong)
        val accountBrowseButton = accountBrowseButton(message.idLong)
        val downtimeEditButton = downtimeEditButton(message.idLong)
        val characterDeleteButton = characterDeleteButton(message.idLong)
        val cancelButton = cancelButton(message.idLong)
        message.editActionComponents(accountAddButton, accountBrowseButton, downtimeEditButton, characterDeleteButton, cancelButton).setContent(null).queue {
            eventWaiter.waitForButton(accountAddButton, user) { addAccount(it, character) }
            eventWaiter.waitForButton(accountBrowseButton, user) { interaction -> interaction.deferEdit().queue { accountPaginator(character.accounts, it, user) } }
            eventWaiter.waitForButton(downtimeEditButton, user) {
                downtimeSupport.configureDowntime(it, character.downtimeOverride.asDowntimeFields()) { event, fields ->
                    saveDowntime(event, character, fields)
                }
            }
            eventWaiter.waitForButton(characterDeleteButton, user) { deleteCharacter(it, character) }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    private fun accountMenu(message: Message, account: AccountEntity, user: User) {
        val accountDepositButton = accountDepositButton(message.idLong)
        val accountEditButton = accountEditButton(message.idLong, account.balance)
        val accountWithdrawButton = accountWithdrawButton(message.idLong)
        val accountDeleteButton = accountDeleteButton(message.idLong, account.accountType.bank == null)
        val cancelButton = cancelButton(message.idLong)
        message.editActionComponents(accountEditButton, accountDeleteButton, cancelButton).setContent(null).queue {
            eventWaiter.waitForButton(accountDepositButton, user) { deposit(it, account)}
            eventWaiter.waitForButton(accountEditButton, user) { editAccount(it, account) }
            eventWaiter.waitForButton(accountWithdrawButton, user) { withdraw(it, account) }
            eventWaiter.waitForButton(accountDeleteButton, user) { deleteAccount(it, account) }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    @Transactional
    fun characterPaginator(characters: List<CharacterEntity>, hook: InteractionHook, user: User, selectFunction: (Message, CharacterEntity, User) -> Unit) {
        lateinit var character: CharacterEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete().queue()
                else selectFunction(message, character, user)
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
                if (message.hasTimedOut()) message.delete().queue()
                else thread {
                    Thread.sleep(2000)
                    accountMenu(message, account, user)
                }
            }
            .setEventWaiter(eventWaiter)
            .setTimeout(5, MINUTES)
            .clearItems()
            .addItems(accounts.map(AccountEntity::asEmbed))
            .setText { p, _ ->
                account = accounts[p - 1]
                "Select account:"
            }.build()
            .display(hook)
    }

    private fun deleteCharacter(event: ButtonInteractionEvent, character: CharacterEntity) {
        val modal = deleteCharacterModal(character, event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferReply().queue { hook ->
                val reason = e.interaction.getValue(reasonField.id)?.asString?.takeUnless(String::isBlank)
                    ?.prepend(" Reason: ") ?: ""
                bankService.deleteCharacter(character)
                hook.editOriginal("$character deleted!$reason").setComponents().setEmbeds().queue()
            }
        }
    }

    private fun deleteAccount(event: ButtonInteractionEvent, account: AccountEntity) {
        val modal = deleteAccountModal(account, event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue { hook ->
                val reason = e.interaction.getValue(reasonField.id)?.asString
                bankService.deleteAccount(account)
                hook.editOriginal("${account.fullName()} deleted!".let {
                    if (reason != null) "$it\nReason: $reason"
                    else it
                }).setComponents().setEmbeds().queue()
            }
        }
    }


    fun addCharacter(event: ButtonInteractionEvent, target: User?) {
        val modal = characterAddModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val name = e.interaction.getValue(characterNameField.id)!!.asString
                val bank = bankService.getBank(e.guild!!)
                val character = CharacterEntity(
                    bank = bank,
                    playerId = target?.idLong,
                    name = name
                )
                characterRepository.saveAndFlush(character)
                val walletType = accountTypeRepository.getByBankNull()
                accountEntityRepository.saveAndFlush(AccountEntity(0, character, walletType))
                it.editOriginal("Character $character added.").setComponents().setEmbeds().queue()
            }
        }
    }

    private fun addAccount(event: ButtonInteractionEvent, character: CharacterEntity) {
        val accountTypes = bankService.getBank(event.guild!!).accountTypes.filterNot(character::hasAccount)
        if (accountTypes.isEmpty()) event.editMessage("No available account types.").queue()
        else {
            val accountTypeField = AccountTypeField(event.id, accountTypes)
            event.editMessage(
                MessageEditBuilder().setContent("Select account type to add:")
                    .setActionRow(accountTypeField).setEmbeds().build()
            ).queue()
            eventWaiter.waitForSelection(accountTypeField, event.user) { interaction ->
                interaction.deferEdit().queue { hook ->
                    val type = interaction.values.first()
                    val accountType = accountTypes.first { it.id.toString() == type }
                    val account = AccountEntity(character = character, accountType = accountType)
                    character.accounts.add(account)
                    characterRepository.saveAndFlush(character)
                    hook.editOriginal("$account account added to $character.").setComponents().setEmbeds().queue()
                }
            }
        }
    }

    private fun deposit(event: ButtonInteractionEvent, account: AccountEntity) {
        val modal = accountDepositModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    if (num > 0) {
                        account.deposit(num, e.user)
                        characterRepository.saveAndFlush(account.character)
                        it.editOriginal("$num deposited to ${account.fullName()}.").setComponents().setEmbeds()
                            .queue()
                    } else {
                        it.editOriginal("Deposit value must be a positive number.")
                    }
                } catch (_: NumberFormatException) {
                    it.editOriginal("$value is not a valid number.").queue()
                }
            }
        }
    }

    private fun editAccount(event: ButtonInteractionEvent, account: AccountEntity) {
        val modal = accountEditModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    account.set(num, e.user)
                    characterRepository.saveAndFlush(account.character)
                    it.editOriginal("${account.fullName()} balance set to $num.").setComponents().setEmbeds().queue()
                } catch (_: NumberFormatException) {
                    it.editOriginal("$value is not a valid number.").queue()
                }
            }
        }
    }

    private fun withdraw(event: ButtonInteractionEvent, account: AccountEntity) {
        val modal = accountWithdrawModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit().queue {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    if (num > 0) {
                        account.withdraw(num, e.user)
                        characterRepository.saveAndFlush(account.character)
                        it.editOriginal("$num withdrawn from ${account.fullName()}.").setComponents().setEmbeds()
                            .queue()
                    } else {
                        it.editOriginal("Withdraw value must be a positive number.")
                    }
                } catch (_: NumberFormatException) {
                    it.editOriginal("$value is not a valid number.").queue()
                }
            }
        }
    }

    private fun saveDowntime(event: ButtonInteractionEvent, character: CharacterEntity, downtimeFields: DowntimeFields) {
        event.deferEdit().queue {
            val characterEntity = characterRepository.getReferenceById(character.id)
            characterEntity.downtimeOverride.applyFields(downtimeFields)
            characterRepository.saveAndFlush(characterEntity)
            it.editOriginal("Downtime overrides saved.").setComponents().setEmbeds().queue()
        }
    }

    companion object {
        private val characterNameField =
            TextInput.create("character_name", "Name", SHORT).setMaxLength(25).setPlaceholder("John Doe").setRequired(true).build()
        private val goldField = TextInput.create("gold", "Enter value:", SHORT).setPlaceholder("0.00").setRequired(true).build()
        private val reasonField = TextInput.create("delete_reason", "Reason", SHORT).setRequired(false).build()
        private fun characterDeleteButton(triggerId: Long) = ButtonImpl("delete_character_$triggerId", "Delete character", DANGER, false, null)
        private fun accountAddButton(triggerId: Long) = ButtonImpl("add_account_$triggerId", "Add account", SUCCESS, false, null)
        private fun accountBrowseButton(triggerId: Long) = ButtonImpl("browse_account_$triggerId", "Browse accounts", SECONDARY, false, null)
        private fun accountDepositButton(triggerId: Long) = ButtonImpl("deposit_account_$triggerId", "Deposit", SUCCESS, false, null)
        private fun accountEditButton(triggerId: Long, balance: Double) = ButtonImpl("edit_account_$triggerId", "Balance:\n$balance gp", PRIMARY, false, null)
        private fun accountWithdrawButton(triggerId: Long) = ButtonImpl("withdraw_account_$triggerId", "Withdraw", DANGER, false, null)
        private fun accountDeleteButton(triggerId: Long, disabled: Boolean) = ButtonImpl("delete_account_$triggerId", "Delete account", DANGER, disabled, null)
        private fun downtimeEditButton(triggerId: Long) = ButtonImpl("edit_downtime_$triggerId", "Edit downtime", PRIMARY, false, null)
        private fun characterAddModal(triggerId: Long) =
            Modal.create("deposit_${triggerId}", "Add character").addActionRow(characterNameField).build()
        private fun accountEditModal(triggerId: Long) =
            Modal.create("edit_${triggerId}", "Edit balance").addActionRow(goldField).build()
        private fun accountDepositModal(triggerId: Long) =
            Modal.create("deposit_${triggerId}", "Deposit").addActionRow(goldField).build()
        private fun accountWithdrawModal(triggerId: Long) =
            Modal.create("withdraw_${triggerId}", "Withdraw").addActionRow(goldField).build()

        private fun deleteCharacterModal(character: CharacterEntity, triggerId: Long) =
            Modal.create("delete_$triggerId", "Delete ${character.name.delim(35)}?").addActionRow(reasonField).build()
        private fun deleteAccountModal(account: AccountEntity, triggerId: Long) = Modal.create("delete_$triggerId", "Delete $account account of ${account.character}".delim()).addActionRow(reasonField).build()
    }
}