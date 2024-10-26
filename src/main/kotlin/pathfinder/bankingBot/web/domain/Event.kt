package pathfinder.bankingBot.web.domain

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

data class Event(
    val type: String,
    val timestamp: LocalDateTime,
    val data: JsonNode ? = null
)
