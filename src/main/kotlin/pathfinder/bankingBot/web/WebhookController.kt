package pathfinder.bankingBot.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import pathfinder.bankingBot.web.domain.Webhook

@Controller("/api/webhook")
class WebhookController {

    @PostMapping
    fun webhook(
        @RequestBody webhook: Webhook,
    ): ResponseEntity<String> = if (webhook.type) ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    else ResponseEntity.noContent().build()
}