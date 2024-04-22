package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.listeners.support.waitForModal
import pathfinder.bankingBot.service.BankService
import pathfinder.diceSyntax.DiceParser
import pathfinder.diceSyntax.components.DiceParseException

@Service
class AccountTypeCommand(
    private val bankService: BankService,
//    private val accountTypeRepository: AccountTypeRepository,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "add_account_type", "Add an account type to the bank.") {
    private val diceParser = DiceParser()

    override fun execute(event: SlashCommandEvent) {
        val modal = accountTypeModal(event.idLong)
        event.replyModal(modal).queue()
        eventWaiter.waitForModal(modal, event.interaction) { e ->
            e.deferReply(false).queue { hook ->
                val name = e.interaction.getValue(nameField.id)!!.asString
                val dice = e.interaction.getValue(diceField.id)!!.asString
                try {
                    diceParser.parse(dice)
                    val bank = bankService.getBank(e.guild!!)
                    bank.accountTypes.add(AccountTypeEntity(0, bank, name, dice))
//                    val accountType =
                    bankService.persist(bank)
//                    accountTypeRepository.saveAndFlush(accountType)
                    hook.editOriginal("Account type $name created.").queue()
                } catch (_: DiceParseException) {
                    hook.setEphemeral(true).editOriginal("$dice is not valid dice syntax.").queue()
                }
            }
        }
    }

    companion object {
        private val nameField =
            TextInput.create("name", "Name", TextInputStyle.SHORT).setPlaceholder("Name").setRequired(true).build()
        private val diceField =
            TextInput.create("dice", "Multiplier", TextInputStyle.PARAGRAPH).setPlaceholder("1d20+1").setRequired(true).build()
        private fun accountTypeModal(triggerId: Long) = Modal.create("account_type_$triggerId", "Account Type:")
            .addActionRow(nameField)
            .addActionRow(diceField).build()
    }
}