package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.command.UserContextMenuEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository
import pathfinder.bankingBot.listeners.inheritance.SendInterface

@Service
class SendMenu(
    override val characterRepository: CharacterRepository,
    override val eventWaiter: EventWaiter
) : SendInterface, UserContextMenu() {

    init {
        name = "Send money"
    }

    override fun execute(event: UserContextMenuEvent) {
        event.deferReply(true).queue { hook ->
            val target = event.targetMember!!
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.user.idLong)
            if (characters.isEmpty())
                hook.editOriginal("You do not have any characters.").queue()
            else {
                val targetCharacters =
                    characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, target.user.idLong)
                sendMenu(event, hook, characters, targetCharacters)
            }
        }
    }
}