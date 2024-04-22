package pathfinder.bankingBot.service

import net.dv8tion.jda.api.JDA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pathfinder.bankingBot.banking.jpa.LogEntity
import javax.persistence.PostPersist

@Service
class LogService {
    @Autowired
    fun setJDA(jda: JDA) {
        LogService.jda = jda
    }

    @PostPersist
    private fun log(logEntity: LogEntity) {
        val bank = logEntity.account.character.bank
        val guild = jda.getGuildById(bank.id)!!
        if (bank.logChannel != null) {
            val channel = guild.getTextChannelById(bank.logChannel!!)
            channel?.sendMessage("${logEntity.account.fullName()} - ${logEntity.description}")?.queue()
        }
    }

    companion object {
        lateinit var jda: JDA
    }
}