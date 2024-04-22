package pathfinder.bankingBot.banking.jpa

import java.io.Serializable
import javax.persistence.*
import javax.persistence.GenerationType.IDENTITY

@Entity
@Table(name = "ACCOUNT_TYPES")
class AccountTypeEntity(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long,
    @ManyToOne
    @JoinColumn(name = "BANK_ID")
    val bank: BankEntity,
    val name: String,
    var interestRate: String): Serializable {
    override fun toString() = name
}
