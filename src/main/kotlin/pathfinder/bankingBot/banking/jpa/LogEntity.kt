package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import pathfinder.bankingBot.service.LoggerService
import java.time.OffsetDateTime

@Entity
@EntityListeners(LoggerService::class)
@Table(name = "LOGS")
class LogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "ACCOUNT_ID", nullable = true)
    var account: AccountEntity?,
    @Column(nullable = false)
    val description: String,
    @Column(nullable = false)
    val balance: Double,
    @Column(nullable = false)
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    override fun toString() = description
}
