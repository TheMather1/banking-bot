package pathfinder.bankingBot.config

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventConfig {

    @Bean
    fun eventWaiter() = EventWaiter()

}