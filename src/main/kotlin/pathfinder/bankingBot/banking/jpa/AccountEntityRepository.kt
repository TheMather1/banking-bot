package pathfinder.bankingBot.banking.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface AccountEntityRepository : JpaRepository<AccountEntity, Long> {

    fun countByAccountType(accountTypeEntity: AccountTypeEntity): Int
}