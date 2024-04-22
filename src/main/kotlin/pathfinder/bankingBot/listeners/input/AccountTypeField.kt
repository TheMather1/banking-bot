package pathfinder.bankingBot.listeners.input

import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity

class AccountTypeField(parentId: String, accountTypes: List<AccountTypeEntity>, maxValues: Int = 1) :
    StringSelectMenu by StringSelectMenu.create(idFor(parentId))
        .addOptions(accountTypes.map { SelectOption.of(it.name, it.id.toString()) }).setPlaceholder("Account type").setMaxValues(maxValues).build() {

    companion object {
        const val id = "accountType"
        fun idFor(command: String) = "$command.$id"
    }
}