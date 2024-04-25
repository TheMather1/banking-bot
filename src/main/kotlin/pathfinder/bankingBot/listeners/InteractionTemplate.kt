package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import pathfinder.bankingBot.listeners.support.ButtonIdentifier
import pathfinder.bankingBot.listeners.support.ModalIdentifier
import pathfinder.bankingBot.listeners.support.SelectMenuIdentifier
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

interface InteractionTemplate {

    fun Message.hasTimedOut() = (timeEdited ?: timeCreated)
        .isAfter(OffsetDateTime.now().plusMinutes(5))

    private inline fun <reified T : Event> EventWaiter.waitForEvent(
        noinline condition: (T) -> Boolean,
        timeout: Long = 5,
        unit: TimeUnit = TimeUnit.MINUTES,
        timeoutAction: Runnable? = null,
        noinline action: (T) -> Unit
    ) = waitForEvent(T::class.java, condition, action, timeout, unit, timeoutAction)

    fun EventWaiter.waitForButton(
        button: Button,
        user: User,
        timeout: Long = 5,
        unit: TimeUnit = TimeUnit.MINUTES,
        timeoutAction: Runnable? = null,
        action: (ButtonInteractionEvent) -> Unit
    ): Unit = waitForEvent<ButtonInteractionEvent>(
        ButtonIdentifier(button, user),
        timeout,
        unit,
        timeoutAction,
        action
    )

    fun EventWaiter.waitForModal(
        modal: Modal,
        interaction: Interaction,
        timeout: Long = 5,
        unit: TimeUnit = TimeUnit.MINUTES,
        timeoutAction: Runnable? = null,
        action: (ModalInteractionEvent) -> Unit
    ) = waitForEvent<ModalInteractionEvent>(
        ModalIdentifier(modal, interaction),
        timeout,
        unit,
        timeoutAction,
        action
    )

    fun EventWaiter.waitForSelection(
        selectMenu: StringSelectMenu,
        user: User,
        timeout: Long = 5,
        unit: TimeUnit = TimeUnit.MINUTES,
        timeoutAction: Runnable? = null,
        action: (StringSelectInteractionEvent) -> Unit
    ) = waitForEvent<StringSelectInteractionEvent>(
        SelectMenuIdentifier(selectMenu, user),
        timeout,
        unit,
        timeoutAction
    ) {
        action(it)
    }
    fun EventWaiter.waitForCancelButton(button: Button, user: User) {
        waitForButton(button, user) { it.deleteMessage() }
    }
    private fun ButtonInteractionEvent.deleteMessage() { this.deferEdit().queue { it.deleteOriginal().queue() }}

    fun cancelButton(triggerId: Long) = ButtonImpl("cancel_$triggerId", "Cancel", ButtonStyle.DANGER, false, null)
}