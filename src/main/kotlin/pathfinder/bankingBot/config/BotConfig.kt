package pathfinder.bankingBot.config

import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.command.SlashCommand
import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MEMBERS
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

//import pathfinder.bankingBot.listeners.BankCommand
//import pathfinder.bankingBot.listeners.CommonListener

@Configuration
@ConfigurationProperties("pathfinder.banking.bot")
class BotConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var token: String


    @Bean
    fun commandClient(slashCommands: Array<SlashCommand>, contextMenu: Array<UserContextMenu>): CommandClient =
        CommandClientBuilder().setOwnerId(108971970074279936L).addSlashCommands(*slashCommands).addContextMenus(*contextMenu).build()

    @Bean
    fun bot(commandClient: CommandClient, eventWaiter: EventWaiter) =
        JDABuilder.createDefault(token, GUILD_MEMBERS).setActivity(Activity.playing("the market"))
            .setRequestTimeoutRetry(false).setMaxReconnectDelay(32)
            .build().apply {
                addEventListener(commandClient, eventWaiter)
                awaitReady()
            }
}
