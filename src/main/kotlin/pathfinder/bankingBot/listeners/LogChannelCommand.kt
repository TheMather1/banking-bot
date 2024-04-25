package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import org.springframework.stereotype.Service
import pathfinder.bankingBot.channelOption
import pathfinder.bankingBot.service.BankService

@Service
class LogChannelCommand(
    private val bankService: BankService,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "log_channel", "Log transactions to a channel.") {

    init {
        options.add(channelOption)
    }

    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val bank = bankService.getBank(event.guild!!)
            val channel = event.getOption(channelOption.name)?.asChannel
            bank.logChannel = channel?.idLong
            bankService.persist(bank)
            if (channel == null) hook.editOriginal("Logging disabled.").queue()
            else hook.editOriginal("Logging to ${channel.asMention}.").queue()
        }
    }
}