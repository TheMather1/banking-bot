package pathfinder.bankingBot.web.domain

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER

data class Webhook(
    val version: Int,
    @field:JsonAlias("application_id")
    val applicationId: String,
    @field:JsonFormat(shape = NUMBER)
    val type: Boolean,
    val event: Event?
)
