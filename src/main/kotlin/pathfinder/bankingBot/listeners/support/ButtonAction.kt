package pathfinder.bankingBot.listeners.support

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.internal.interactions.component.ButtonImpl

class ButtonAction(id: String, label: String, style: ButtonStyle, val action: ButtonInteractionEvent.() -> Unit) {
    val button = ButtonImpl(id, label, style, false, null)
}