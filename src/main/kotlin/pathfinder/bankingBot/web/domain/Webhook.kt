package pathfinder.bankingBot.web.domain

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER

data class Webhook(
    val version: Int,
    @JsonAlias("application_id")
    val applicationId: String?,
    @JsonFormat(shape = NUMBER)
    val type: Boolean,
    val event: Event?
)
