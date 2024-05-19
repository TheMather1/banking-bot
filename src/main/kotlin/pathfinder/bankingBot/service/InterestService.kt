package pathfinder.bankingBot.service

import net.dv8tion.jda.api.JDA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.repository.CharacterRepository
import java.time.OffsetDateTime

@Service
class InterestService(val jda: JDA, val characterRepository: CharacterRepository) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Suppress("LoggingStringTemplateAsArgument")
    @Scheduled(cron = "0 0 0 * * *")
    fun applyInterest() {
        log.debug("Applying interest.")
        val date = OffsetDateTime.now()
        val characters = jda.guilds.flatMap { guild ->
            log.trace("Bank: ${guild.name}")
            characterRepository.findAllByBank_Id(guild.idLong).onEach { character ->
                log.trace("-Character: $character")
                character.accounts.forEach {
                    if (it.accountType.frequency.matches(date)) {
                        it.interest()
                        log.trace("--Account: $it - APPLIED")
                    } else log.trace("--Account: $it - SKIPPED")
                }
            }
        }
        characterRepository.saveAllAndFlush(characters)
    }
}