package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Denomination.GP
import pathfinder.bankingBot.banking.Frequency
import pathfinder.bankingBot.banking.Frequency.WEEKLY
import pathfinder.bankingBot.listeners.input.DowntimeFields
import java.io.Serializable

@Entity
@Table(name = "DOWNTIME_CONFIG")
class DowntimeConfig(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long = 0,
    @OneToOne
    @JoinColumn(name = "BANK_ID", nullable = false)
    val bank: BankEntity,
    @Column(nullable = false)
    var times: Int = 1,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var frequency: Frequency = WEEKLY,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var denomination: Denomination = GP,
    @Column
    var multiplier: Double = 0.5,
    @Column
    var baseDice: String = "1d20"
): Serializable {
    fun asDowntimeFields() = DowntimeFields(times, frequency, denomination, multiplier, baseDice)
    fun applyFields(downtimeFields: DowntimeFields) {
        times = downtimeFields.times ?: 1
        frequency = downtimeFields.frequency ?: WEEKLY
        denomination = downtimeFields.denomination ?: GP
        multiplier = downtimeFields.multiplier ?: 0.5
        baseDice = downtimeFields.baseDice ?: "1d20"
    }
}
