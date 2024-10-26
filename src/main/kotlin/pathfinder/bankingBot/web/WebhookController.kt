package pathfinder.bankingBot.web


import com.iwebpp.crypto.TweetNaclFast
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pathfinder.bankingBot.web.domain.Webhook

@RestController
@RequestMapping("/api/webhook")
class WebhookController {

    val publicKey = "18439c59d508a67fc45e6a0409c37143cef84b30467851e331a32153c79f531a".toByteArray()
    val nacl = TweetNaclFast.Signature(publicKey, ByteArray(0))

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