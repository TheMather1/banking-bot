package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.command.UserContextMenuEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.ButtonEmbedPaginator
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
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
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.deferEdit
import pathfinder.bankingBot.editActionComponents
import pathfinder.bankingBot.listeners.input.AccountTypeField
import pathfinder.bankingBot.listeners.support.waitForButton
import pathfinder.bankingBot.listeners.support.waitForModal
import pathfinder.bankingBot.listeners.support.waitForSelection
import pathfinder.bankingBot.prepend
import pathfinder.bankingBot.service.BankService
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.concurrent.thread

@Service
class PlayerModificationMenu(
    private val bankService: BankService,
    private val characterRepository: CharacterRepository,
    private val eventWaiter: EventWaiter
) : UserContextMenu() {

    init {
        name = "Modify bank details"
    }

    override fun execute(event: UserContextMenuEvent) {
        event.deferReply().queue { hook ->
            val target = event.targetMember!!
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, target.user.idLong)
            val characterAddButton = characterAddButton(event.idLong)
            val cancelButton = cancelButton(event.idLong)
            if (characters.isEmpty()) hook
                .editOriginal("${target.effectiveName} has no characters.")
                .setActionRow(characterAddButton, cancelButton).queue {
                    eventWaiter.waitForButton(characterAddButton, event.user, action = ::addCharacter)
                    eventWaiter.waitForButton(cancelButton, event.user) { it.message.delete().queue() }
                }
            else {
                val characterBrowseButton = characterBrowseButton(event.idLong)
                hook.editOriginalEmbeds(target.toEmbed(characters))
                    .setActionRow(characterAddButton, characterBrowseButton, cancelButton)
                    .queue {
                        eventWaiter.waitForButton(characterAddButton, event.user, action = ::addCharacter)
                        eventWaiter.waitForButton(characterBrowseButton, event.user) { interaction ->
                            interaction.deferEdit().queue { characterPaginator(characters, it, interaction.user) }
                        }
                        eventWaiter.waitForButton(cancelButton, event.user) { it.message.delete().queue() }
                    }
            }
        }
    }

    private fun Member.toEmbed(characters: List<CharacterEntity>) = EmbedBuilder().setTitle(effectiveName)
        .setThumbnail(effectiveAvatarUrl)
        .apply {
            characters.forEach { addField(it.name, it.accounts.joinToString(", "), false) }
        }.build()


    private fun characterMenu(message: Message, character: CharacterEntity, user: User) {
        val accountAddButton = accountAddButton(message.idLong)
        val accountBrowseButton = accountBrowseButton(message.idLong)
        val characterDeleteButton = characterDeleteButton(message.idLong)
        val cancelButton = cancelButton(message.idLong)
        if (character.accounts.isEmpty()) {
            message.editActionComponents(accountAddButton, characterDeleteButton, cancelButton).setContent(null).queue {
                eventWaiter.waitForButton(accountAddButton, user) { addAccount(it, character) }
                eventWaiter.waitForButton(characterDeleteButton, user) { deleteCharacter(it, character) }
                eventWaiter.waitForButton(cancelButton, user) { it.message.delete().queue() }
            }
        } else {
            message.editActionComponents(accountAddButton, accountBrowseButton, characterDeleteButton, cancelButton).setContent(null).queue {
                eventWaiter.waitForButton(accountAddButton, user) { addAccount(it, character) }
                eventWaiter.waitForButton(accountBrowseButton, user) { interaction -> interaction.deferEdit().queue { accountPaginator(character.accounts, it, user) } }
                eventWaiter.waitForButton(characterDeleteButton, user) { deleteCharacter(it, character) }
                eventWaiter.waitForButton(cancelButton, user) { it.message.delete().queue() }
            }
        }
    }

    private fun accountMenu(message: Message, account: AccountEntity, user: User) {
        val accountEditButton = accountEditButton(message.idLong)
        val accountDeleteButton = accountDeleteButton(message.idLong)
        val cancelButton = cancelButton(message.idLong)
        message.editActionComponents(accountEditButton, accountDeleteButton, cancelButton).setContent(null).queue {
            eventWaiter.waitForButton(accountEditButton, user) { editAccount(it, account) }
            eventWaiter.waitForButton(accountDeleteButton, user) { deleteAccount(it, account) }
            eventWaiter.waitForButton(cancelButton, user) { it.message.delete().queue() }
        }
    }

    private fun characterPaginator(characters: List<CharacterEntity>, hook: InteractionHook, user: User) {
        lateinit var character: CharacterEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete().queue()
                else characterMenu(message, character, user)
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
                characterRepository.delete(character)
                characterRepository.flush()
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
                val character = account.character
                character.accounts.remove(account)
                characterRepository.saveAndFlush(character)
        //            accountRepository.delete(account)
        //            accountRepository.flush()
                hook.editOriginal("${account.fullName()} deleted!".let {
                    if (reason != null) "$it\nReason: $reason"
                    else it
                }).setComponents().setEmbeds().queue()
            }
        }
    }

    private fun addCharacter(event: ButtonInteractionEvent) {
        val modal = characterAddModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit {
                val name = e.interaction.getValue(characterNameField.id)!!.asString
                val bank = bankService.getBank(e.guild!!)
                val character = CharacterEntity(
                    bank = bank,
                    playerId = e.user.idLong,
                    name = name
                )
                characterRepository.saveAndFlush(character)
                queue {
                    it.editOriginal("Character $character added.").setComponents().setEmbeds().queue()
                }
            }
        }
    }

    private fun addAccount(event: ButtonInteractionEvent, character: CharacterEntity) {
        val accountTypes = bankService.getBank(event.guild!!).accountTypes.filter {  type ->
            character.accounts.none { it.accountType.id == type.id }
        }
        if (accountTypes.isEmpty()) event.editMessage("No available account types.").queue()
        else {
            val accountTypeField = AccountTypeField(event.id, accountTypes)
            event.editMessage(
                MessageEditBuilder().setContent("Select account type to add:")
                    .setActionRow(accountTypeField).setEmbeds().build()
            ).queue()
            eventWaiter.waitForSelection(accountTypeField, event.user) { interaction ->
                interaction.deferEdit {
                    val type = interaction.values.first()
                    val accountType = accountTypes.first { it.id.toString() == type }
                    val account = AccountEntity(0, character, accountType)
//                accountRepository.saveAndFlush(account)
                    character.accounts.add(account)
                    characterRepository.saveAndFlush(character)
                    queue {
                        it.editOriginal("$account account added to $character.").setComponents().setEmbeds().queue()
                    }
                }
            }
        }
    }

    private fun editAccount(event: ButtonInteractionEvent, account: AccountEntity) {
        val modal = accountEditModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event) { e ->
            e.deferEdit {
                val value = e.interaction.getValue(goldField.id)!!.asString
                try {
                    val num = value.toDouble()
                    account.set(num, e.user)
                    characterRepository.saveAndFlush(account.character)
                    queue {
                        it.editOriginal("${account.fullName()} balance set to $num.").setComponents().setEmbeds().queue()
                    }
                } catch (_: NumberFormatException) {
                    queue {
                        it.editOriginal("$value is not a valid number.").queue()
                    }
                }
            }
        }
    }

    fun Message.hasTimedOut() = (timeEdited ?: timeCreated)
        .isAfter(OffsetDateTime.now().plusMinutes(5))

    companion object {
        private val characterNameField =
            TextInput.create("character_name", "Name", SHORT).setPlaceholder("John Doe").setRequired(true).build()
        private val goldField = TextInput.create("gold", "Enter value:", SHORT).setPlaceholder("0.00").setRequired(true).build()
        private val reasonField = TextInput.create("delete_reason", "Reason", SHORT).setRequired(false).build()
        private fun characterBrowseButton(triggerId: Long) = ButtonImpl("browse_character_$triggerId", "Browse characters", PRIMARY, false, null)
        private fun characterAddButton(triggerId: Long) = ButtonImpl("add_character_$triggerId", "Add character", SUCCESS, false, null)
        private fun characterDeleteButton(triggerId: Long) = ButtonImpl("delete_character_$triggerId", "Delete character", DANGER, false, null)
        private fun accountAddButton(triggerId: Long) = ButtonImpl("add_account_$triggerId", "Add account", SUCCESS, false, null)
        private fun accountBrowseButton(triggerId: Long) = ButtonImpl("browse_account_$triggerId", "Browse accounts", PRIMARY, false, null)
        private fun accountEditButton(triggerId: Long) = ButtonImpl("edit_account_$triggerId", "Edit balance", PRIMARY, false, null)
        private fun accountDeleteButton(triggerId: Long) = ButtonImpl("delete_account_$triggerId", "Delete account", DANGER, false, null)
        private fun cancelButton(triggerId: Long) = ButtonImpl("cancel_$triggerId", "Cancel", DANGER, false, null)
        private fun characterAddModal(triggerId: Long) =
            Modal.create("deposit_${triggerId}", "Add character").addActionRow(characterNameField).build()
        private fun accountEditModal(triggerId: Long) =
            Modal.create("deposit_${triggerId}", "Edit balance").addActionRow(goldField).build()

        private fun deleteCharacterModal(character: CharacterEntity, triggerId: Long) =
            Modal.create("delete_$triggerId", "Delete $character?").addActionRow(reasonField).build()
        private fun deleteAccountModal(account: AccountEntity, triggerId: Long) = Modal.create("delete_$triggerId", "Delete $account account of ${account.character}").addActionRow(reasonField).build()
    }
}