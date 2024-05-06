package pathfinder.bankingBot.service.support

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Denomination.GP
import pathfinder.bankingBot.banking.Frequency
import pathfinder.bankingBot.banking.Frequency.WEEKLY
import pathfinder.bankingBot.listeners.InteractionTemplate
import pathfinder.bankingBot.listeners.input.DowntimeFields
import pathfinder.diceSyntax.DiceParser
import pathfinder.diceSyntax.components.DiceParseException

@Service
class DowntimeSupport(val eventWaiter: EventWaiter, val diceParser: DiceParser): InteractionTemplate {

    fun configureDowntime(
        event: GenericComponentInteractionCreateEvent,
        downtimeFields: DowntimeFields,
        saveAction: (ButtonInteractionEvent, DowntimeFields) -> Unit
    ) {
        event.deferEdit().queue {
            configureDowntime(
                it,
                event.idLong,
                event.user,
                downtimeFields,
                saveAction
            )
        }
    }

    fun configureDowntime(
        event: ModalInteractionEvent,
        downtimeFields: DowntimeFields,
        saveAction: (ButtonInteractionEvent, DowntimeFields) -> Unit
    ) {
        event.deferEdit().queue {
            configureDowntime(
                it,
                event.idLong,
                event.user,
                downtimeFields,
                saveAction
            )
        }
    }

    fun configureDowntime(
        hook: InteractionHook,
        eventId: Long,
        user: User,
        downtimeFields: DowntimeFields,
        saveAction: (ButtonInteractionEvent, DowntimeFields) -> Unit
    ) {
        val timesSelectMenu = timesSelectMenu(eventId, downtimeFields.times ?: 1)
        val frequencySelectMenu = frequencySelectMenu(eventId, downtimeFields.frequency ?: WEEKLY)
        val denominationSelectMenu = denominationSelectMenu(eventId, downtimeFields.denomination ?: GP)
        val configSaveButton = configSaveButton(eventId)
        val multiplierEditButton = multiplierEditButton(eventId, downtimeFields.multiplier ?: 0.5)
        val baseDiceEditButton = baseDiceEditButton(eventId, downtimeFields.baseDice ?: "1d20")
        val cancelButton = cancelButton(eventId)

        hook.editOriginalComponents(
            ActionRow.of(timesSelectMenu),
            ActionRow.of(frequencySelectMenu),
            ActionRow.of(denominationSelectMenu),
            ActionRow.of(configSaveButton, baseDiceEditButton, multiplierEditButton, cancelButton)
        ).queue {
            eventWaiter.waitForSelection(timesSelectMenu, user) { event ->
                downtimeFields.times = event.valueUnlessDefault { it.toInt() }
                configureDowntime(event, downtimeFields, saveAction)
            }
            eventWaiter.waitForSelection(frequencySelectMenu, user) { event ->
                downtimeFields.frequency = event.valueUnlessDefault { Frequency.valueOf(it) }
                configureDowntime(event, downtimeFields, saveAction)
            }
            eventWaiter.waitForSelection(denominationSelectMenu, user) { event ->
                downtimeFields.denomination = event.valueUnlessDefault { Denomination.valueOf(it) }
                configureDowntime(event, downtimeFields, saveAction)
            }
            eventWaiter.waitForButton(configSaveButton, user) { event ->
                saveAction(event, downtimeFields)
            }
            eventWaiter.waitForButton(multiplierEditButton, user) { event ->
                val multiplierModal = multiplierModal(event.idLong, downtimeFields.multiplier)
                event.replyModal(multiplierModal).queue()
                eventWaiter.waitForModal(multiplierModal, event.interaction) { modalEvent ->
                    try {
                        downtimeFields.multiplier = modalEvent.getValue("multiplier")?.asString?.toDouble()
                    } catch (_: NumberFormatException) { }
                    configureDowntime(modalEvent, downtimeFields, saveAction)
                }
            }
            eventWaiter.waitForButton(baseDiceEditButton, user) { event ->
                val baseDiceModal = baseDiceModal(event.idLong, downtimeFields.baseDice)
                event.replyModal(baseDiceModal).queue()
                eventWaiter.waitForModal(baseDiceModal, event.interaction) { modalEvent ->
                    try {
                        downtimeFields.baseDice = modalEvent.getValue("multiplier")?.asString?.also {
                            diceParser.parse(it)
                        }
                    } catch (_: DiceParseException) { }
                    configureDowntime(modalEvent, downtimeFields, saveAction)
                }
            }
            eventWaiter.waitForCancelButton(cancelButton, user)
        }
    }

    private fun <T> StringSelectInteraction.valueUnlessDefault(transform: (String) -> T): T? = values.first().mapUnlessDefault(transform)
    private fun <T> String.mapUnlessDefault(transform: (String) -> T): T? = takeUnless { it == "default"}?.let(transform)

    companion object {
        private fun configSaveButton(triggerId: Long) = ButtonImpl("save_config_$triggerId", "Save changes",
            ButtonStyle.SUCCESS, false, null)
        private fun multiplierEditButton(triggerId: Long, multiplier: Double) = ButtonImpl("multiplier_$triggerId",
            "Multiplier:\n${multiplier}x",
            ButtonStyle.PRIMARY, false, null)
        private fun baseDiceEditButton(triggerId: Long, baseDice: String) = ButtonImpl("base_dice_$triggerId", "Base dice:\n$baseDice",
            ButtonStyle.PRIMARY, false, null)
        private fun timesSelectMenu(triggerId: Long, default: Int) = StringSelectMenu.create("select_times_$triggerId")
            .addOptions((1..24).map { SelectOption.of("$it times", it.toString()) })
            .setDefaultValues(default.toString()).build()
        private fun frequencySelectMenu(triggerId: Long, default: Frequency?) = StringSelectMenu.create("select_frequency_$triggerId")
            .addOptions(Frequency.entries.map { SelectOption.of(it.name, it.name) } + SelectOption.of("default", "default"))
            .setDefaultValues(default?.name ?: "default").build()
        private fun denominationSelectMenu(triggerId: Long, default: Denomination) = StringSelectMenu.create("select_denomination_$triggerId")
            .addOptions(Denomination.entries.map { SelectOption.of(it.name, it.name) })
            .setDefaultValues(default.name).build()

        private fun multiplierField(default: Double?) =
            TextInput.create("multiplier", "Multiplier", TextInputStyle.SHORT).setPlaceholder("0.5").setRequired(false).setValue(default?.toString()).build()
        private fun multiplierModal(triggerId: Long, default: Double?) = Modal.create("multiplier_$triggerId", "Downtime multiplier:")
            .addActionRow(multiplierField(default)).build()
        private fun baseDiceField(default: String?) =
            TextInput.create("baseDice", "Base dice", TextInputStyle.SHORT).setPlaceholder("1d20").setRequired(false).setValue(default).build()
        private fun baseDiceModal(triggerId: Long, default: String?) = Modal.create("base_dice_$triggerId", "Downtime dice:")
            .addActionRow(baseDiceField(default)).build()
    }
}