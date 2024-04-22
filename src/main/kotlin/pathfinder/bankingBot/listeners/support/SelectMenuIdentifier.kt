package pathfinder.bankingBot.listeners.support

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class SelectMenuIdentifier(selectMenu: StringSelectMenu, user: User) : (StringSelectInteraction) -> Boolean {
    private val id = selectMenu.id
    private val userId = user.idLong

    override fun invoke(trigger: StringSelectInteraction) =
        trigger.selectMenu.id!! == id
                && trigger.user.idLong == userId
}