package pathfinder.bankingBot.web


import com.iwebpp.crypto.TweetNaclFast
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import pathfinder.bankingBot.web.domain.Webhook

@Controller("/api/webhook")
class WebhookController {

    val publicKey = "".toByteArray()
    val nacl = TweetNaclFast.Signature(ByteArray(0), publicKey)

    @PostMapping
    fun webhook(
        @RequestHeader("X-Signature-Ed25519") signature: ByteArray,
        @RequestHeader("X-Signature-Timestamp") timestamp: String,
        @RequestBody webhook: Webhook,
        @RequestBody rawBody: String
    ): ResponseEntity<String> = when {
        !nacl.detached_verify("$timestamp$rawBody".toByteArray(), signature) -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        webhook.type -> ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
        else -> ResponseEntity.noContent().build()
    }
}