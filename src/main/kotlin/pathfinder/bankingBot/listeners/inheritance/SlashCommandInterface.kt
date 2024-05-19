package pathfinder.bankingBot.listeners.inheritance

import com.jagrosh.jdautilities.command.SlashCommand
import com.jagrosh.jdautilities.commons.waiter.EventWaiter


abstract class SlashCommandInterface(protected open val eventWaiter: EventWaiter, name: String, help: String): InteractionTemplate, SlashCommand() {

    init {
        super.name = name
        super.help = help
    }
}
