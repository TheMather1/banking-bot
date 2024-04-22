package pathfinder.bankingBot.banking.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface BankRepository: JpaRepository<BankEntity, Long> {
}