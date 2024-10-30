package pathfinder.bankingBot.config

import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.command.SlashCommand
import com.jagrosh.jdautilities.command.UserContextMenu
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MEMBERS
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
        JDABuilder.createDefault(token, GUILD_MEMBERS)
            .disableCache(VOICE_STATE, EMOJI, STICKER, SCHEDULED_EVENTS)
            .setRequestTimeoutRetry(false).setMaxReconnectDelay(32)
            .build().apply {
                addEventListener(commandClient, eventWaiter)
                logger.info("Adding commands: ${commandClient.slashCommands.map { it.name }}")
                logger.info("Adding context menus: ${commandClient.contextMenus.map { it.name }}")
                awaitReady()
                presence.setPresence(Activity.playing("the market"), true)
            }
}
