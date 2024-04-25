package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import pathfinder.bankingBot.service.LogService
import java.time.OffsetDateTime

@Entity
@EntityListeners(LogService::class)
@Table(name = "LOGS")
class LogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(targetEntity = AccountEntity::class)
    @JoinColumn(name = "ACCOUNT_ID")
    val account: AccountEntity,
    val description: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    override fun toString() = description
}
