package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommand
import com.jagrosh.jdautilities.commons.waiter.EventWaiter


abstract class SlashCommandInterface(protected val eventWaiter: EventWaiter, name: String, help: String): SlashCommand() {

    init {
        super.name = name
        super.help = help
    }
}
