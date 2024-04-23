package pathfinder.bankingBot.banking.jpa

import java.time.DayOfWeek.MONDAY
import java.time.OffsetDateTime

enum class Frequency {
    NEVER {
        override fun matches(offsetDateTime: OffsetDateTime) = false
    },
    DAILY {
        override fun matches(offsetDateTime: OffsetDateTime) = true
    },
    WEEKLY {
        override fun matches(offsetDateTime: OffsetDateTime) = offsetDateTime.dayOfWeek == MONDAY
    },
    MONTHLY {
        override fun matches(offsetDateTime: OffsetDateTime) = offsetDateTime.dayOfMonth == 1
    };

    abstract fun matches(offsetDateTime: OffsetDateTime): Boolean
}