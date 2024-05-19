package pathfinder.bankingBot.banking.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pathfinder.bankingBot.banking.jpa.BankEntity

@Repository
interface BankRepository: JpaRepository<BankEntity, Long>