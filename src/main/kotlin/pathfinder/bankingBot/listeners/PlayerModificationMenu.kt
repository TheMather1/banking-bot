package pathfinder.bankingBot.listeners

import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.command.UserContextMenuEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.bankingBot.service.support.CharacterSupport

@Service
class PlayerModificationMenu(
    private val characterRepository: CharacterRepository,
    private val eventWaiter: EventWaiter,
    private val characterSupport: CharacterSupport
) : InteractionTemplate, UserContextMenu() {

    init {
        name = "Manage characters"
    }

    override fun execute(event: UserContextMenuEvent) {
        event.deferReply().queue { hook ->
            val target = event.targetMember!!
            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, target.user.idLong)
            val characterAddButton = characterAddButton(event.idLong)
            val cancelButton = cancelButton(event.idLong)
            if (characters.isEmpty()) hook
                .editOriginal("${target.effectiveName} has no characters.")
                .setActionRow(characterAddButton, cancelButton).queue {
                    eventWaiter.waitForButton(characterAddButton, event.user) { characterSupport.addCharacter(it, event.target)}
                    eventWaiter.waitForCancelButton(cancelButton, event.user)
                }
            else {
                val characterBrowseButton = characterBrowseButton(event.idLong)
                hook.editOriginalEmbeds(target.toEmbed(characters))
                    .setActionRow(characterAddButton, characterBrowseButton, cancelButton)
                    .queue {
                        eventWaiter.waitForButton(characterAddButton, event.user) { characterSupport.addCharacter(it, event.target)}
                        eventWaiter.waitForButton(characterBrowseButton, event.user) { interaction ->
                            interaction.deferEdit().queue { characterSupport.characterPaginator(characters, it, interaction.user) }
                        }
                        eventWaiter.waitForCancelButton(cancelButton, event.user)
                    }
            }
        }
    }

    private fun Member.toEmbed(characters: List<CharacterEntity>) = EmbedBuilder().setTitle(effectiveName)
        .setThumbnail(effectiveAvatarUrl)
        .apply {
            characters.forEach { addField(it.name, it.accounts.joinToString(", "), false) }
        }.build()

    companion object {
        private fun characterBrowseButton(triggerId: Long) = ButtonImpl("browse_character_$triggerId", "Browse characters", PRIMARY, false, null)
        private fun characterAddButton(triggerId: Long) = ButtonImpl("add_character_$triggerId", "Add character", SUCCESS, false, null)
    }
}