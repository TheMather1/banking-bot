package pathfinder.bankingBot.service

import net.dv8tion.jda.api.JDA
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class InterestService(val jda: JDA, val bankService: BankService) {

    @Scheduled(cron = "0 0 0 * * *")
    fun applyInterest() {
        val date = OffsetDateTime.now()
        jda.guilds.map(bankService::getBank).forEach {  bank ->
            bank.characters.forEach { character ->
                character.accounts.forEach {
                    if (it.accountType.frequency.matches(date)) it.interest()
                }
            }
            bankService.persist(bank)
        }
    }
}