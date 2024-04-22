package pathfinder.bankingBot.service

import net.dv8tion.jda.api.entities.Guild
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.BankEntity
import pathfinder.bankingBot.banking.jpa.BankRepository

@Service
class BankService(val bankRepository: BankRepository) {

    fun getBank(id: Long): BankEntity = bankRepository.findByIdOrNull(id) ?: bankRepository.saveAndFlush(BankEntity(id))

    fun getBank(guild: Guild): BankEntity = getBank(guild.idLong)
    fun persist(bank: BankEntity) {
        bankRepository.saveAndFlush(bank)
    }
}