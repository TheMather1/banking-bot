package pathfinder.bankingBot.banking.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity

@Repository
interface AccountTypeRepository : JpaRepository<AccountTypeEntity, Long> {

    fun findByBankNull(): AccountTypeEntity?
    fun getByBankNull(): AccountTypeEntity
}