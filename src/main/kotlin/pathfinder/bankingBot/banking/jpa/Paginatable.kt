package pathfinder.bankingBot.banking.jpa

import net.dv8tion.jda.api.entities.MessageEmbed

interface Paginatable {
    fun asEmbed(): MessageEmbed
}
