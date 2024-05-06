package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntityRepository
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.truncateToCopper
import pathfinder.diceSyntax.DiceParser
import pathfinder.diceSyntax.components.DiceParseException
import pathfinder.diceSyntax.plus
import pathfinder.diceSyntax.times

@Service
class DowntimeCommand(
    private val characterRepository: CharacterRepository,
    private val accountEntityRepository: AccountEntityRepository,
    private val diceParser: DiceParser,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "downtime", "Perform downtime") {



    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.user.idLong)
            selectCharacter(characters, characters.first(), hook)
        }
    }

    private fun selectCharacter(characters: List<CharacterEntity>, character: CharacterEntity, hook: InteractionHook) {
        val characterField = characterField(hook.idLong, characters, character)
        val selectButton = selectButton(hook.idLong)
        val cancelButton = cancelButton(hook.idLong)
        hook.editOriginalComponents(ActionRow.of(characterField), ActionRow.of(selectButton, cancelButton)).queue()
        eventWaiter.waitForSelection(characterField, hook.interaction.user) { event ->
            event.deferEdit().queue {
                val selectedCharacterId = event.selectedOptions.first().value.toLong()
                val selectedCharacter = characters.first { it.id == selectedCharacterId }
                selectCharacter(characters, selectedCharacter, it)
            }
        }
        eventWaiter.waitForButton(selectButton, hook.interaction.user) { buttonInteractionEvent ->
            val downtimeModal = downtimeModal(buttonInteractionEvent.idLong)
            buttonInteractionEvent.replyModal(downtimeModal).queue {
                eventWaiter.waitForModal(downtimeModal, buttonInteractionEvent) {
                    performDowntime(it, character)
                }
            }
        }
        eventWaiter.waitForCancelButton(cancelButton, hook.interaction.user)
    }

    private fun performDowntime(event: ModalInteractionEvent, character: CharacterEntity) {
        try {
            val bonus = diceParser.parse(event.getValue(bonusField.id)!!.asString)
//                try {

//                DiceNumber(event.getValue(bonusField.id)!!.asString.toInt())
//            } catch (_: NumberFormatException) {
//            }
            val activity = event.getValue(activityField.id)!!.asString
            event.deferEdit().queue {
                character.downtimeOverride.asDowntimeFields()
                    .withDefaults(character.bank.downtimeConfig).apply {
                        val dice = diceParser.parse(baseDice!!).plus(bonus).times(multiplier!!)
                        val rolls = (1..times!!).map {
                            truncateToCopper(dice.toDouble(), denomination!!)
                        }
                        val walletId = character.accounts.first { it.accountType.bank == null }.id
                        val wallet = accountEntityRepository.getReferenceById(walletId)
                        wallet.earn(rolls.sum(), denomination!!, activity, "${times}x $dice")
                        accountEntityRepository.saveAndFlush(wallet)
                        it.editOriginal("(${times}x $dice $rolls)\n$character spends a ${frequency!!.singular} $activity, earning ${rolls.sum()} ${denomination}.")
                            .setComponents().queue()
                    }
            }
        } catch (_: DiceParseException) {
            event.editMessage("Invalid bonus: ${event.getValue(bonusField.id)?.asString}").queue()
        }
    }

    companion object {
        private val bonusField = TextInput.create("bonus", "Bonus", SHORT).setPlaceholder("0").setRequired(true).build()
        private val activityField = TextInput.create("activity", "Activity", SHORT).setPlaceholder("chopping wood").setRequired(true).build()
        fun characterField(id: Long, characters: List<CharacterEntity>, default: CharacterEntity) = StringSelectMenu.create("character_$id")
            .addOptions(characters.map { SelectOption.of(it.name, it.id.toString()) })
            .setDefaultValues(default.id.toString())
            .setPlaceholder("Character").setMaxValues(1).build()
        fun selectButton(id: Long) = ButtonImpl("select_$id", "Select", ButtonStyle.PRIMARY, false, null)
        fun downtimeModal(id: Long) = Modal.create("downtime_$id", "Perform downtime").addActionRow(bonusField).addActionRow(
            activityField).build()

    }
}