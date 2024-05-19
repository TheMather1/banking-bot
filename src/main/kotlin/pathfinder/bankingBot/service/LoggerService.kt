package pathfinder.bankingBot.service

import jakarta.persistence.PostPersist
import net.dv8tion.jda.api.JDA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.LogEntity

@Service
class LoggerService {
    @Autowired
    fun setJDA(jda: JDA) {
        LoggerService.jda = jda
    }

    @PostPersist
    private fun log(logEntity: LogEntity) {
        val bank = logEntity.account!!.character.bank
        val guild = jda.getGuildById(bank.id)!!
        if (bank.logChannel != null) {
            val channel = guild.getTextChannelById(bank.logChannel!!)
            channel?.sendMessage("${logEntity.account!!.fullName()} - ${logEntity.description}\nCurrent balance: ${logEntity.balance}")?.queue()
        }
    }

    companion object {
        lateinit var jda: JDA
    }
}