package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Frequency
import pathfinder.bankingBot.listeners.input.DowntimeFields
import java.io.Serializable

@Entity
@Table(name = "DOWNTIME_OVERRIDE")
class DowntimeOverride(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long,
    @OneToOne
    @JoinColumn(name = "CHARACTER_ID", nullable = false)
    val character: CharacterEntity,
    @Column(nullable = true)
    var times: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var frequency: Frequency? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var denomination: Denomination? = null,
    @Column(nullable = true)
    var multiplier: Double? = null,
    @Column(nullable = true)
    var baseDice: String? = null
): Serializable{
    fun asDowntimeFields() = DowntimeFields(times, frequency, denomination, multiplier, baseDice)
    fun applyFields(downtimeFields: DowntimeFields) {
        times = downtimeFields.times
        frequency = downtimeFields.frequency
        denomination = downtimeFields.denomination
        multiplier = downtimeFields.multiplier
        baseDice = downtimeFields.baseDice
    }
}
