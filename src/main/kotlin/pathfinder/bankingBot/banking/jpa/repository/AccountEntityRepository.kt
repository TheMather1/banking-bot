package pathfinder.bankingBot.banking.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pathfinder.bankingBot.banking.jpa.AccountEntity
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity

@Repository
interface AccountEntityRepository : JpaRepository<AccountEntity, Long> {

    fun countByAccountType(accountTypeEntity: AccountTypeEntity): Int
}