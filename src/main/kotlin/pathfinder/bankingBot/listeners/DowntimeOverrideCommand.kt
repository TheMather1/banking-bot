//package pathfinder.bankingBot.listeners
//
//import com.jagrosh.jdautilities.command.UserContextMenu
//import com.jagrosh.jdautilities.command.UserContextMenuEvent
//import com.jagrosh.jdautilities.commons.waiter.EventWaiter
//import net.dv8tion.jda.api.entities.Message
//import net.dv8tion.jda.api.entities.User
//import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
//import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.DANGER
//import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
//import net.dv8tion.jda.internal.interactions.component.ButtonImpl
//import org.springframework.stereotype.Service
//import pathfinder.bankingBot.banking.jpa.CharacterEntity
//import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository
//import pathfinder.bankingBot.editActionComponents
//import pathfinder.bankingBot.listeners.inheritance.InteractionTemplate
//import pathfinder.bankingBot.listeners.input.DowntimeFields
//import pathfinder.bankingBot.service.support.CharacterSupport
//import pathfinder.bankingBot.service.support.DowntimeSupport
//
//@Service
//class DowntimeOverrideCommand(
//    private val characterRepository: CharacterRepository,
//    private val downtimeSupport: DowntimeSupport,
//    private val characterSupport: CharacterSupport,
//    private val eventWaiter: EventWaiter
//) : UserContextMenu(), InteractionTemplate {
//
//    init {
//        name = "Modify downtime override"
//    }
//
//    override fun execute(event: UserContextMenuEvent) {
//        event.deferReply().queue {
//            val characters = characterRepository.getByBank_IdAndPlayerId(event.guild!!.idLong, event.target.idLong)
//            characterSupport.characterPaginator(characters, it, event.user, ::characterSelect)
//        }
//    }
//
//    fun characterSelect(message: Message, character: CharacterEntity, user: User) {
//        val editButton = editButton(message.idLong)
//        val deleteButton = deleteButton(message.idLong)
//        val cancelButton = cancelButton(message.idLong)
//        message.editActionComponents(editButton).queue {
//            eventWaiter.waitForButton(editButton, user) {
//                downtimeSupport.configureDowntime(
//                    it.hook,
//                    it.idLong,
//                    user,
//                    character.downtimeOverride.asDowntimeFields()
//                ) { event, downtimeFields ->
//                    saveConfig(event, downtimeFields, character.id)
//                }
//            }
//            eventWaiter.waitForButton(deleteButton, user) {
//                deleteConfig(it, character.id)
//            }
//            eventWaiter.waitForCancelButton(cancelButton, user)
//        }
//
//    }
//
//    private fun saveConfig(
//        event: ButtonInteractionEvent,
//        downtimeFields: DowntimeFields,
//        characterId: Long
//    ) {
//        event.deferEdit().queue { hook ->
//            val character = characterRepository.getReferenceById(
//                characterId
//            )
//            character.downtimeOverride.applyFields(downtimeFields)
//            characterRepository.saveAndFlush(character)
//            hook.editOriginal("Updated downtime override for ${character.name}.").setEmbeds().setComponents().queue()
//        }
//    }
//
//    private fun deleteConfig(event: ButtonInteractionEvent, characterId: Long) {
//        event.deferEdit().queue { hook ->
//            val character = characterRepository.getReferenceById(
//                characterId
//            )
//            character.downtimeOverride.apply {
//                times = null
//                frequency = null
//                denomination = null
//                multiplier = null
//                baseDice = null
//            }
//            characterRepository.saveAndFlush(character)
//            hook.editOriginal("${character.name} reset to default downtime configuration.").setEmbeds().setComponents().queue()
//        }
//    }
//
//    companion object {
//        private fun editButton(triggerId: Long) = ButtonImpl("edit_$triggerId", "Edit override", SUCCESS, false, null)
//        private fun deleteButton(triggerId: Long) = ButtonImpl("delete_$triggerId", "Delete override", DANGER, false, null)
//    }
//}