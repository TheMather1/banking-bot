package pathfinder.bankingBot.listeners.support

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction

class ButtonIdentifier(button: Button, user: User) : (ButtonInteraction) -> Boolean {
    private val id = button.id
    private val userId = user.idLong

    override fun invoke(trigger: ButtonInteraction) =
        trigger.button.id!! == id
                && trigger.user.idLong == userId
}