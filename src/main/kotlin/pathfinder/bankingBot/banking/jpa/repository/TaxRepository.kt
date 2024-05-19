package pathfinder.bankingBot.banking.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.banking.jpa.TaxConfig

@Repository
interface TaxRepository: JpaRepository<TaxConfig, Long> {
    fun findBySourceAccountType(sourceAccountType: AccountTypeEntity): TaxConfig?
}