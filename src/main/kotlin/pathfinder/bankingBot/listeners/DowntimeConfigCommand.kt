package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Service
import pathfinder.bankingBot.listeners.input.DowntimeFields
import pathfinder.bankingBot.banking.jpa.service.BankService
import pathfinder.bankingBot.listeners.inheritance.SlashCommandInterface
import pathfinder.bankingBot.service.support.DowntimeSupport

@Service
class DowntimeConfigCommand(
    private val bankService: BankService,
    private val downtimeSupport: DowntimeSupport,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "downtime_config", "Modify downtime configuration") {



    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue {
            val config = bankService.getBank(event.guild!!).downtimeConfig
            downtimeSupport.configureDowntime(
                it,
                event.idLong,
                event.user,
                config.asDowntimeFields(),
                ::saveConfig
            )
        }
    }

    private fun saveConfig(
        event: ButtonInteractionEvent,
        downtimeFields: DowntimeFields
    ) {
        event.deferEdit().queue { hook ->
            val bank = bankService.getBank(event.guild!!)
            bank.downtimeConfig.applyFields(downtimeFields)
            bankService.persist(bank)
            hook.editOriginal("Updated downtime config.").setEmbeds().setComponents().queue()
        }
    }
}