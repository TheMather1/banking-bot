package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.ButtonEmbedPaginator
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntityRepository
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.banking.jpa.Frequency
import pathfinder.bankingBot.listeners.support.waitForButton
import pathfinder.bankingBot.listeners.support.waitForModal
import pathfinder.bankingBot.listeners.support.waitForSelection
import pathfinder.bankingBot.service.BankService
import pathfinder.diceSyntax.DiceParser
import pathfinder.diceSyntax.components.DiceParseException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Service
class AccountTypeCommand(
    private val bankService: BankService,
    private val accountEntityRepository: AccountEntityRepository,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "account_types", "View and modify account types.") {
    private val diceParser = DiceParser()



    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val bank = bankService.getBank(event.guild!!)
            val accountTypes = bank.accountTypes
            val accountTypeAddButton = accountTypeAddButton(event.idLong)
            val cancelButton = cancelButton(event.idLong)
            if (accountTypes.isEmpty()) hook
                .editOriginal("There are no registered account types.")
                .setActionRow(accountTypeAddButton, cancelButton).queue {
                    eventWaiter.waitForButton(accountTypeAddButton, event.user, action = ::addAccountType)
                    eventWaiter.waitForButton(cancelButton, event.user) { it.message.delete().queue() }
                }
            else {
                val accountTypeBrowseButton = accountTypeBrowseButton(event.idLong)
                hook.editOriginalEmbeds(toEmbed(accountTypes))
                    .setActionRow(accountTypeAddButton, accountTypeBrowseButton, cancelButton)
                    .queue {
                        eventWaiter.waitForButton(accountTypeAddButton, event.user, action = ::addAccountType)
                        eventWaiter.waitForButton(accountTypeBrowseButton, event.user) { interaction ->
                            interaction.deferEdit().queue { accountTypePaginator(accountTypes, it, interaction.user) }
                        }
                        eventWaiter.waitForButton(cancelButton, event.user) { it.message.delete().queue() }
                    }
            }
        }
    }

    fun toEmbed(accountTypes: List<AccountTypeEntity>) = EmbedBuilder().setTitle("Account types").apply {
        accountTypes.forEach { addField(it.name, "${it.interestRate} ${it.frequency}", false) }
    }.build()

    fun addAccountType(event: ButtonInteractionEvent) {
        val modal = accountTypeModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event.interaction) { e ->
            e.deferEdit().queue { hook ->
                val name = e.interaction.getValue(nameField().id)!!.asString
                val dice = e.interaction.getValue(diceField().id)!!.asString
                try {
                    diceParser.parse(dice)
                    val bank = bankService.getBank(e.guild!!)
                    bank.accountTypes.add(AccountTypeEntity(0, bank, name, dice))
                    bankService.persist(bank)
                    hook.editOriginal("Account type $name created.").queue()
                } catch (_: DiceParseException) {
                    val retryButton = retryButton(e.idLong)
                    val cancelButton = cancelButton(e.idLong)
                    hook.editOriginal("$dice is not valid dice syntax.").setEmbeds().setComponents(ActionRow.of(retryButton, cancelButton)).queue()
                    eventWaiter.waitForButton(retryButton, e.user) { addAccountType(it) }
                    eventWaiter.waitForButton(cancelButton, e.user) { it.message.delete().queue() }
                }
            }
        }
    }



    private fun accountTypePaginator(accountTypes: List<AccountTypeEntity>, hook: InteractionHook, user: User) {
        lateinit var accountType: AccountTypeEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete().queue()
                else thread {
                    Thread.sleep(2000)
                    accountTypeMenu(message, message.idLong, accountType, user, accountType.frequency)
                }
            }
            .setEventWaiter(eventWaiter)
            .setTimeout(5, TimeUnit.MINUTES)
            .clearItems()
            .addItems(accountTypes.map {
                val count = accountEntityRepository.countByAccountType(it)
                it.asEmbed(count)
            } )
            .setText { p, _ ->
                accountType = accountTypes[p - 1]
                "Select account:"
            }.build()
            .display(hook)
    }

    private fun accountTypeMenu(message: Message, id: Long, accountTypeEntity: AccountTypeEntity, user: User, selectedFrequency: Frequency) {
        val count = accountEntityRepository.countByAccountType(accountTypeEntity)
        val frequencySelectMenu = frequencySelector(id, selectedFrequency)
        val accountTypeEditButton = accountTypeEditButton(id)
        val accountTypeSaveButton = accountTypeSaveButton(id, selectedFrequency == accountTypeEntity.frequency)
        val accountTypeDeleteButton = accountTypeDeleteButton(id, count > 0)
        val cancelButton = cancelButton(id)
        message.editMessageComponents(
            ActionRow.of(frequencySelectMenu),
            ActionRow.of(accountTypeSaveButton, accountTypeEditButton, accountTypeDeleteButton, cancelButton)
        ).setContent(null).queue {
            eventWaiter.waitForSelection(frequencySelectMenu, user) { e ->
                e.deferEdit().queue {
                    accountTypeMenu(message, it.interaction.idLong, accountTypeEntity, user, Frequency.valueOf(e.selectedOptions.first().value))
                }
            }
            eventWaiter.waitForButton(accountTypeSaveButton, user) { e ->
                e.deferEdit().queue { hook ->
                    val bank = bankService.getBank(message.guild)
                    val refreshedEntity = bank.accountTypes.first { it.id == accountTypeEntity.id }
                    refreshedEntity.frequency = selectedFrequency
                    bankService.persist(bank)
                    message.editMessageEmbeds(refreshedEntity.asEmbed(count)).queue()
                    accountTypeMenu(message, hook.interaction.idLong, refreshedEntity, user, selectedFrequency)
                }
            }
            eventWaiter.waitForButton(accountTypeEditButton, user) { editAccountType(it, accountTypeEntity) }
            eventWaiter.waitForButton(accountTypeDeleteButton, user) { deleteAccountType(it, accountTypeEntity) }
            eventWaiter.waitForButton(cancelButton, user) { it.message.delete().queue() }
        }
    }

    private fun editAccountType(event: ButtonInteractionEvent, accountType: AccountTypeEntity) {
        val modal = accountTypeModal(event.idLong, accountType.name, accountType.interestRate)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event.interaction) { e ->
            e.deferEdit().queue { hook ->
                val name = e.interaction.getValue(nameField().id)!!.asString
                val dice = e.interaction.getValue(diceField().id)!!.asString
                try {
                    diceParser.parse(dice)
                    val bank = bankService.getBank(e.guild!!)
                    val entity = bank.accountTypes.first()
                    entity.name = name
                    entity.interestRate = dice
                    bankService.persist(bank)
                    hook.editOriginal("Account type $name updated.").queue()
                } catch (_: DiceParseException) {
                    val retryButton = retryButton(e.idLong)
                    val cancelButton = cancelButton(e.idLong)
                    hook.editOriginal("$dice is not valid dice syntax.").setEmbeds().setComponents(ActionRow.of(retryButton, cancelButton)).queue()
                    eventWaiter.waitForButton(retryButton, e.user) { editAccountType(it, accountType) }
                    eventWaiter.waitForButton(cancelButton, e.user) { it.message.delete().queue() }
                }
            }
        }
    }

    private fun deleteAccountType(event: ButtonInteractionEvent, accountType: AccountTypeEntity) {
        event.deferEdit().queue { hook ->
            val bank = bankService.getBank(event.guild!!)
            bank.accountTypes.removeIf { it.id == accountType.id }
            bankService.persist(bank)
            hook.editOriginal("Account type $accountType deleted.").setEmbeds().setComponents().queue()
        }

    }

    fun Message.hasTimedOut() = (timeEdited ?: timeCreated)
        .isAfter(OffsetDateTime.now().plusMinutes(5))

    companion object {
        private fun accountTypeAddButton(triggerId: Long) = ButtonImpl("add_account_type_$triggerId", "Add account type", ButtonStyle.SUCCESS, false, null)
        private fun cancelButton(triggerId: Long) = ButtonImpl("cancel_$triggerId", "Cancel",
            ButtonStyle.DANGER, false, null)
        private fun accountTypeBrowseButton(triggerId: Long) = ButtonImpl("browse_account_types_$triggerId", "Browse account types",
            ButtonStyle.PRIMARY, false, null)
        private fun accountTypeEditButton(triggerId: Long) = ButtonImpl("edit_account_type_$triggerId", "Edit interest",
            ButtonStyle.PRIMARY, false, null)
        private fun accountTypeSaveButton(triggerId: Long, disabled: Boolean) = ButtonImpl("save_account_type_$triggerId", "Save changes",
            ButtonStyle.PRIMARY, disabled, null)
        private fun accountTypeDeleteButton(triggerId: Long, disabled: Boolean) = ButtonImpl("delete_account_type_$triggerId", "Delete account type",
            ButtonStyle.DANGER, disabled, null)

        private fun retryButton(triggerId: Long) = ButtonImpl("retry_$triggerId", "Retry", ButtonStyle.PRIMARY, false, null)
        private fun frequencySelector(triggerId: Long, default: Frequency) = StringSelectMenu.create("select_frequencu_$triggerId")
            .addOptions(Frequency.entries.map { SelectOption.of(it.name, it.name) }).setDefaultValues(default.name).build()

        private fun nameField(default: String? = null) =
            TextInput.create("name", "Name", TextInputStyle.SHORT).setPlaceholder("Name").setRequired(true).setValue(default).build()
        private fun diceField(default: String? = null) =
            TextInput.create("dice", "Multiplier", TextInputStyle.PARAGRAPH).setPlaceholder("1d20+1").setRequired(true).setValue(default).build()
        private fun accountTypeModal(triggerId: Long, defaultName: String? = null, defaultDice: String? = null) = Modal.create("account_type_$triggerId", "Account Type:")
            .addActionRow(nameField(defaultName))
            .addActionRow(diceField(defaultDice)).build()
    }
}