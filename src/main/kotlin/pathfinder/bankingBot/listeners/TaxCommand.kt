package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.menu.ButtonEmbedPaginator
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
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
import pathfinder.bankingBot.banking.TransactionType
import pathfinder.bankingBot.banking.jpa.*
import pathfinder.bankingBot.service.BankService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Service
class TaxCommand(
    private val bankService: BankService,
    private val characterRepository: CharacterRepository,
    private val taxRepository: TaxRepository,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "tax_config", "Assign and modify tax rates for account types") {



    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val bank = bankService.getBank(event.guild!!)
            val accountTypes = bank.accountTypes
            val cancelButton = cancelButton(event.idLong)
            if (accountTypes.isEmpty()) hook
                .editOriginal("There are no registered account types.")
                .setActionRow(cancelButton).queue {
                    eventWaiter.waitForCancelButton(cancelButton, event.user)
                }
            else {
                accountTypePaginator(accountTypes, hook, event.user)
            }
        }
    }

    fun toEmbed(accountTypes: List<AccountTypeEntity>) = EmbedBuilder().setTitle("Tax config").apply {
        accountTypes.forEach {
            addField("Applies to", it.name, false)
            if(it.taxConfig != null) {
                addField("Rate", "${it.taxConfig!!.percentage}%", true)
                addField("On action", it.taxConfig!!.actions.joinToString(", "), true)
                addField("Subtracts from sum", it.taxConfig!!.subtract.toString(), true)
                addField("Recipient", it.taxConfig!!.targetAccount.fullName(), false)
            }
        }
    }.build()

    private fun accountTypePaginator(accountTypes: List<AccountTypeEntity>, hook: InteractionHook, user: User) {
        lateinit var accountType: AccountTypeEntity
        ButtonEmbedPaginator.Builder().waitOnSinglePage(true)
            .setUsers(user)
            .setFinalAction { message ->
                if (message.hasTimedOut()) message.delete().queue()
                else thread {
                    Thread.sleep(2000)
                    taxMenu(message, message.idLong, accountType, user)
                }
            }
            .setEventWaiter(eventWaiter)
            .setTimeout(5, TimeUnit.MINUTES)
            .clearItems()
            .addItems(toEmbed(accountTypes))
            .setText { p, _ ->
                accountType = accountTypes[p - 1]
                "Select account:"
            }.build()
            .display(hook)
    }

    private fun taxMenu(message: Message, id: Long, accountTypeEntity: AccountTypeEntity, user: User) {
        val npcs = characterRepository.getByBank_IdAndPlayerId(message.guildIdLong, null)
        val taxConfigureButton = taxConfigureButton(id)
        val taxDeleteButton = taxDeleteButton(id, accountTypeEntity.taxConfig == null)
        val cancelButton = cancelButton(id)
        message.editMessageComponents(
            ActionRow.of(taxConfigureButton, taxDeleteButton, cancelButton)
        ).setContent(null).queue {
            eventWaiter.waitForButton(taxConfigureButton, user) {
                configureTax(
                    it,
                    accountTypeEntity,
                    npcs,
                    accountTypeEntity.taxConfig?.actions,
                    accountTypeEntity.taxConfig?.targetAccount?.character,
                    accountTypeEntity.taxConfig?.targetAccount,
                    accountTypeEntity.taxConfig?.percentage,
                    accountTypeEntity.taxConfig?.subtract ?: false
                )
            }
            eventWaiter.waitForButton(taxDeleteButton, user) { deleteTax(it, accountTypeEntity) }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    fun configureTax(
        originalEvent: GenericComponentInteractionCreateEvent,
        accountType: AccountTypeEntity,
        npcs: List<CharacterEntity>,
        actions: Set<TransactionType>?,
        recipient: CharacterEntity?,
        targetAccount: AccountEntity?,
        percentage: Int?,
        subtract: Boolean
    ) {
        originalEvent.deferEdit().queue { configureTax(
            it,
            originalEvent.idLong,
            originalEvent.user,
            accountType,
            npcs,
            actions,
            recipient,
            targetAccount,
            percentage,
            subtract
        )}
    }

    fun configureTax(
        originalEvent: ModalInteractionEvent,
        accountType: AccountTypeEntity,
        npcs: List<CharacterEntity>,
        actions: Set<TransactionType>?,
        recipient: CharacterEntity?,
        targetAccount: AccountEntity?,
        percentage: Int?,
        subtract: Boolean
    ) {
        originalEvent.deferEdit().queue { configureTax(
            it,
            originalEvent.idLong,
            originalEvent.user,
            accountType,
            npcs,
            actions,
            recipient,
            targetAccount,
            percentage,
            subtract
        )}
    }

    @Suppress("NAME_SHADOWING")
    fun configureTax(
        hook: InteractionHook,
        eventId: Long,
        user: User,
        accountType: AccountTypeEntity,
        npcs: List<CharacterEntity>,
        actions: Set<TransactionType>?,
        recipient: CharacterEntity?,
        targetAccount: AccountEntity?,
        percentage: Int?,
        subtract: Boolean
    ) {
        val actionSelectMenu = actionSelectMenu(eventId, actions)
        val recipientSelectMenu = recipientSelectMenu(eventId, npcs, recipient)
        val accountSelectMenu = accountSelectMenu(eventId, recipient?.accounts, targetAccount, recipient == null)
        val subtractSelectMenu = subtractSelectMenu(eventId, subtract)
        val taxSaveButton = taxSaveButton(eventId, actions.isNullOrEmpty() || recipient == null || targetAccount == null || percentage == null)
        val taxPercentageButton = taxPercentageButton(eventId, percentage)
        val cancelButton = cancelButton(eventId)

        hook.editOriginal("").setComponents(
            ActionRow.of(actionSelectMenu),
            ActionRow.of(recipientSelectMenu),
            ActionRow.of(accountSelectMenu),
            ActionRow.of(subtractSelectMenu),
            ActionRow.of(taxSaveButton, taxPercentageButton, cancelButton)
        ).queue {
            eventWaiter.waitForSelection(actionSelectMenu, user) { event ->
                val actions = event.values.map(TransactionType::valueOf).toSet()
                configureTax(event, accountType, npcs, actions, recipient, targetAccount, percentage, subtract)
            }
            eventWaiter.waitForSelection(recipientSelectMenu, user) { event ->
                val recipientId = event.values.first().toLong()
                val recipient = npcs.first { it.id == recipientId }
                val targetAccount = null
                configureTax(event, accountType, npcs, actions, recipient, targetAccount, percentage, subtract)
            }
            eventWaiter.waitForSelection(accountSelectMenu, user) { event ->
                val targetId = event.values.first().toLong()
                val targetAccount = recipient!!.accounts.first { it.id == targetId }
                configureTax(event, accountType, npcs, actions, recipient, targetAccount, percentage, subtract)
            }
            eventWaiter.waitForSelection(subtractSelectMenu, user) { event ->
                val subtract = event.values.first().toBoolean()
                configureTax(event, accountType, npcs, actions, recipient, targetAccount, percentage, subtract)
            }
            eventWaiter.waitForButton(taxSaveButton, user) { event ->
                saveTax(event, accountType, actions!!, targetAccount!!, percentage!!, subtract)
            }
            eventWaiter.waitForButton(taxPercentageButton, user) { event ->
                val percentageModal = percentageModal(event.idLong, percentage)
                event.replyModal(percentageModal).queue()
                eventWaiter.waitForModal(percentageModal, event.interaction) { modalEvent ->
                    val percentage = try {
                        modalEvent.getValue("percentage")?.asString?.toInt()
                    } catch (_: NumberFormatException) {
                        null
                    }
                    configureTax(modalEvent, accountType, npcs, actions, recipient, targetAccount, percentage, subtract)
                }
            }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    private fun saveTax(
        event: ButtonInteractionEvent,
        accountType: AccountTypeEntity,
        actions: Set<TransactionType>,
        targetAccount: AccountEntity,
        percentage: Int,
        subtract: Boolean
    ) {
        event.deferEdit().queue { hook ->
            val taxConfig = taxRepository.findBySourceAccountType(accountType)?.also {
                it.actions = actions
                it.targetAccount = targetAccount
                it.subtract = subtract
                it.percentage = percentage
            } ?: TaxConfig(
                sourceAccountType = accountType,
                targetAccount = targetAccount,
                percentage = percentage,
                subtract = subtract,
                actions = actions
            )
            taxRepository.saveAndFlush(taxConfig)
            hook.editOriginal("Saved tax config for $accountType.").setEmbeds().setComponents().queue()
        }
    }

    private fun deleteTax(event: ButtonInteractionEvent, accountType: AccountTypeEntity) {
        event.deferEdit().queue { hook ->
            val taxConfig = taxRepository.findBySourceAccountType(accountType)
            if (taxConfig != null) {
                taxRepository.delete(taxConfig)
                taxRepository.flush()
            }
            hook.editOriginal("Tax config for $accountType deleted.").setEmbeds().setComponents().queue()
        }

    }

    companion object {
        private fun taxConfigureButton(triggerId: Long) = ButtonImpl("configure_tax_$triggerId", "Configure tax", ButtonStyle.PRIMARY, false, null)
        private fun taxSaveButton(triggerId: Long, disabled: Boolean) = ButtonImpl("save_tax_$triggerId", "Save changes",
            ButtonStyle.SUCCESS, disabled, null)
        private fun taxDeleteButton(triggerId: Long, disabled: Boolean) = ButtonImpl("delete_tax_type_$triggerId", "Delete tax configuration",
            ButtonStyle.DANGER, disabled, null)
        private fun taxPercentageButton(triggerId: Long, percentage: Int?) = ButtonImpl("percentage_tax_$triggerId", if(percentage != null) "$percentage%" else "Set percentage",
            ButtonStyle.PRIMARY, false, null)
        private fun actionSelectMenu(triggerId: Long, default: Collection<TransactionType>?) = StringSelectMenu.create("select_transaction_$triggerId")
            .addOptions(TransactionType.entries.map { SelectOption.of(it.name, it.name) })
            .setDefaultValues(default?.map(TransactionType::name) ?: emptyList())
            .setMaxValues(TransactionType.entries.size).build()
        private fun recipientSelectMenu(triggerId: Long, options: List<CharacterEntity>, default: CharacterEntity?) = StringSelectMenu.create("select_character_$triggerId")
            .addOptions(options.map { SelectOption.of(it.name, it.id.toString()) })
            .setPlaceholder("Character")
            .setDefaultValues(listOfNotNull(default?.id?.toString()))
            .build()
        private fun accountSelectMenu(triggerId: Long, options: List<AccountEntity>?, default: AccountEntity?, disabled: Boolean) = StringSelectMenu.create("select_account_$triggerId")
            .addOptions(options?.map { SelectOption.of(it.accountType.name, it.id.toString()) } ?: listOf(SelectOption.of("disabled", "disabled")))
            .setPlaceholder("Account")
            .setDefaultValues(listOfNotNull(default?.id?.toString()))
            .setDisabled(disabled)
            .build()
        private fun subtractSelectMenu(triggerId: Long, default: Boolean) = StringSelectMenu.create("select_subtract_$triggerId")
            .addOptions(SelectOption.of("TRUE", true.toString()), SelectOption.of("FALSE", false.toString()))
            .setDefaultValues(default.toString())
            .build()

        private fun percentageField(default: Int? = null) =
            TextInput.create("percentage", "Tax (%)", TextInputStyle.SHORT).setPlaceholder("0%").setRequired(true).setValue(default.toString()).build()
        private fun percentageModal(triggerId: Long, defaultPercentage: Int? = null) = Modal.create("account_type_$triggerId", "Account Type:")
            .addActionRow(percentageField(defaultPercentage)).build()
    }
}