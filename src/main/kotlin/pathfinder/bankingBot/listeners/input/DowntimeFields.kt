package pathfinder.bankingBot.listeners.input

import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Denomination.GP
import pathfinder.bankingBot.banking.Frequency
import pathfinder.bankingBot.banking.Frequency.WEEKLY
import pathfinder.bankingBot.banking.jpa.DowntimeConfig
import java.io.Serializable

data class DowntimeFields(
    var times: Int? = 1,
    var frequency: Frequency? = WEEKLY,
    var denomination: Denomination? = GP,
    var multiplier: Double? = 0.5,
    var baseDice: String? = "1d20"
): Serializable {
    fun withDefaults(downtimeConfig: DowntimeConfig): DowntimeFields {
        if(times == null) times = downtimeConfig.times
        if(frequency == null) frequency = downtimeConfig.frequency
        if(denomination == null) denomination = downtimeConfig.denomination
        if(multiplier == null) multiplier = downtimeConfig.multiplier
        if(baseDice == null) baseDice = downtimeConfig.baseDice
        return this
    }
}
