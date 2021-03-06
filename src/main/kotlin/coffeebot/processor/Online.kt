package coffeebot.processor

import coffeebot.commands.Dispatcher
import coffeebot.database.Log
import coffeebot.message.DiscordMultiChannelHandle
import coffeebot.message.toCoffeeBotMessage
import discord4j.common.close.CloseException
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent

class Online(private val token: String, private val miltonSecret: String?) : MessageProcessor {

    private val log = Log("log.txt")

    override fun run() {
        val client = DiscordClientBuilder(token).build()
        val dispatcher = Dispatcher(log, miltonSecret, DiscordMultiChannelHandle(client))

        client.eventDispatcher.on(ReadyEvent::class.java)
                .subscribe { ready -> println("Logged in as " + ready.self.username) }

        client.eventDispatcher.on(MessageCreateEvent::class.java)
                .subscribe {
                    dispatcher.process(it.toCoffeeBotMessage())
                }

        println("Attempting to log on")
        try {
            client.login().block()
        } catch (e: CloseException) {
            if (4004 == e.closeStatus.code) {
                println("Invalid Token")
            }
        }
    }
}
