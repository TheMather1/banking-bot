package pathfinder.bankingBot.banking.jpa.service

import net.dv8tion.jda.api.entities.Guild
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.banking.jpa.BankEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity
import pathfinder.bankingBot.banking.jpa.repository.BankRepository

@Service
class BankService(val bankRepository: BankRepository) {

    fun getBank(id: Long): BankEntity = bankRepository.findByIdOrNull(id) ?: bankRepository.saveAndFlush(BankEntity(id))

    fun getBanks(ids: List<Long>): MutableList<BankEntity> = bankRepository.findAllById(ids)

    fun getBank(guild: Guild): BankEntity = getBank(guild.idLong)

    fun getBanksByGuild(guilds: List<Guild>): List<BankEntity> = getBanks(guilds.map { it.idLong })
    fun persist(bank: BankEntity): BankEntity = bankRepository.saveAndFlush(bank)

    fun persistAll(banks: List<BankEntity>): MutableList<BankEntity> = bankRepository.saveAllAndFlush(banks)

    fun deleteCharacter(character: CharacterEntity) {
        getBank(character.bank.id).let { bank ->
            bank.characters.removeAll { it.id == character.id }
            persist(bank)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        getBank(account.character.bank.id).let { bank ->
            bank.characters.first { it.id == account.character.id }
                .accounts.removeAll { it.id == account.id }
            persist(bank)
        }
    }

    fun deleteAccountType(accountType: AccountTypeEntity) {
        getBank(accountType.bank!!.id).let { bank ->
            bank.accountTypes.removeAll { it.id == accountType.id }
            persist(bank)
        }
    }
}