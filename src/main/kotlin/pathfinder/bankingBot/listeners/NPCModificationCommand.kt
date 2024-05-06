package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.service.support.CharacterSupport

@Service
class NPCModificationCommand(
    private val characterRepository: CharacterRepository,
    private val characterSupport: CharacterSupport,
    eventWaiter: EventWaiter
) : SlashCommandInterface(eventWaiter, "npc_accounts", "Manage NPCs.") {

    override fun execute(event: SlashCommandEvent) {
        event.deferReply().queue { hook ->
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, null)
            val characterAddButton = characterAddButton(event.idLong)
            val cancelButton = cancelButton(event.idLong)
            if (characters.isEmpty()) hook
                .editOriginal("There are no NPC characters.")
                .setActionRow(characterAddButton, cancelButton).queue {
                    eventWaiter.waitForButton(characterAddButton, event.user) { characterSupport.addCharacter(it, null)}
                    eventWaiter.waitForCancelButton(cancelButton, event.user)
                }
            else {
                val characterBrowseButton = characterBrowseButton(event.idLong)
                hook.editOriginalEmbeds(toEmbed(characters))
                    .setActionRow(characterAddButton, characterBrowseButton, cancelButton)
                    .queue {
                        eventWaiter.waitForButton(characterAddButton, event.user) { characterSupport.addCharacter(it, null)}
                        eventWaiter.waitForButton(characterBrowseButton, event.user) { interaction ->
                            interaction.deferEdit().queue { characterSupport.characterPaginator(characters, it, interaction.user) }
                        }
                        eventWaiter.waitForCancelButton(cancelButton, event.user)
                    }
            }
        }
    }

    private fun toEmbed(characters: List<CharacterEntity>) = EmbedBuilder().setTitle("NPCs")
        .apply {
            characters.forEach { addField(it.name, it.accounts.joinToString(", "), false) }
        }.build()


    companion object {
        private fun characterBrowseButton(triggerId: Long) = ButtonImpl("browse_character_$triggerId", "Browse characters", PRIMARY, false, null)
        private fun characterAddButton(triggerId: Long) = ButtonImpl("add_character_$triggerId", "Add character", SUCCESS, false, null)
    }
}