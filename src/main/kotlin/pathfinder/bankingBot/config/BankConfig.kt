package pathfinder.bankingBot.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pathfinder.bankingBot.banking.Frequency.NEVER
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.banking.jpa.AccountTypeRepository
import pathfinder.bankingBot.banking.jpa.CharacterRepository
import pathfinder.diceSyntax.DiceParser

@Configuration
class BankConfig {

    @Bean
    fun diceParser() = DiceParser()

    @Autowired
    fun addWallet(accountTypeRepository: AccountTypeRepository, characterRepository: CharacterRepository) {
        val walletType = accountTypeRepository.findByBankNull()
            ?: accountTypeRepository.saveAndFlush(AccountTypeEntity(0, null, "Wallet", "0", NEVER))
//        characterRepository.findAll().filter { it.accounts.none { it.accountType.id == walletType.id } }
        val characters = characterRepository.findAllByAccounts_AccountTypeNot(walletType)
            .onEach {
                it.accounts.add(AccountEntity(0, it, walletType))
            }
        characterRepository.saveAllAndFlush(characters)
    }
}