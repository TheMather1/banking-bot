package pathfinder.bankingBot.banking

import java.time.DayOfWeek.MONDAY
import java.time.OffsetDateTime

enum class Frequency(val singular: String) {
    NEVER("never") {
        override fun matches(offsetDateTime: OffsetDateTime) = false
    },
    DAILY("day") {
        override fun matches(offsetDateTime: OffsetDateTime) = true
    },
    WEEKLY("week") {
        override fun matches(offsetDateTime: OffsetDateTime) = offsetDateTime.dayOfWeek == MONDAY
    },
    MONTHLY("month") {
        override fun matches(offsetDateTime: OffsetDateTime) = offsetDateTime.dayOfMonth == 1
    };

    abstract fun matches(offsetDateTime: OffsetDateTime): Boolean
}