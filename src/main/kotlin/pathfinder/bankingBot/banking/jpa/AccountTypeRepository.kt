package pathfinder.bankingBot.banking.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface AccountTypeRepository : JpaRepository<AccountTypeEntity, Long> {

    fun findByBankNull(): AccountTypeEntity?
    fun getByBankNull(): AccountTypeEntity
}