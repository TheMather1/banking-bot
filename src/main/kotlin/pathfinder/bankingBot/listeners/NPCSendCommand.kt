package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository
import pathfinder.bankingBot.listeners.inheritance.SendInterface
import pathfinder.bankingBot.listeners.inheritance.SlashCommandInterface

@Service
class NPCSendCommand(
    override val characterRepository: CharacterRepository,
    override val eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "npc_send", "Send money to an NPC account."), SendInterface {

    override fun execute(event: SlashCommandEvent) {
        event.deferReply(true).queue { hook ->
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.user.idLong)
            val targetCharacters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, null)
            sendMenu(event, hook, characters, targetCharacters)
        }
    }
}