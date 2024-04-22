package pathfinder.bankingBot.listeners.support

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

inline fun <reified T : Event> EventWaiter.waitForEvent(
    noinline condition: (T) -> Boolean,
    timeout: Long = 5,
    unit: TimeUnit = MINUTES,
    timeoutAction: Runnable? = null,
    noinline action: (T) -> Unit
) = waitForEvent(T::class.java, condition, action, timeout, unit, timeoutAction)

fun EventWaiter.waitForButton(
    button: Button,
    user: User,
    timeout: Long = 5,
    unit: TimeUnit = MINUTES,
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
    unit: TimeUnit = MINUTES,
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
    unit: TimeUnit = MINUTES,
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