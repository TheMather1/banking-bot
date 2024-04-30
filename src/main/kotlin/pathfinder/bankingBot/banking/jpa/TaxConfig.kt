package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import pathfinder.bankingBot.banking.TransactionType
import pathfinder.bankingBot.truncateToCopper

@Entity
@Table(name = "TAX_CONFIGS")
class TaxConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @OneToOne(targetEntity = AccountTypeEntity::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "SOURCE_ACCOUNT_TYPE_ID", nullable = false)
    val sourceAccountType: AccountTypeEntity,
    @OneToOne(targetEntity = AccountEntity::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "TARGET_ACCOUNT_ID", nullable = false)
    var targetAccount: AccountEntity,
    @Column(nullable = false)
    var percentage: Int,
    @Column(nullable = false)
    var subtract: Boolean,
    @Convert(converter = TransactionType.Converter::class)
    @Column(nullable = false)
    var actions: Set<TransactionType>
) {

    fun tax(value: Double, action: TransactionType, source: AccountEntity): Double {
        return if (action in actions) {
            val tax = truncateToCopper(value / 100 * percentage)
            targetAccount.receiveTax(tax, source)
            if (subtract) value - tax else value
        } else value
    }
}
