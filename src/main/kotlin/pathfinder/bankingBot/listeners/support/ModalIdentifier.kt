package pathfinder.bankingBot.listeners.support

import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.interactions.modals.ModalInteraction

class ModalIdentifier(modal: Modal, interaction: Interaction) : (ModalInteraction) -> Boolean {
    private val id = modal.id
    private val userId = interaction.user.idLong

    override fun invoke(trigger: ModalInteraction) =
        trigger.modalId == id
                && trigger.user.idLong == userId
}