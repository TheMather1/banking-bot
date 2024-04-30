package pathfinder.bankingBot.banking.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface TaxRepository: JpaRepository<TaxConfig, Long> {
    fun findBySourceAccountType(sourceAccountType: AccountTypeEntity): TaxConfig?
}