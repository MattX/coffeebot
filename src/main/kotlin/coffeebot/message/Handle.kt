package coffeebot.message

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent

interface Handle {
    fun sendMessage(message: String)

    fun react(emoji: String)
}

object NullHandle: Handle {
    override fun sendMessage(message: String) {}

    override fun react(emoji: String) {}
}

class StdoutHandler(private val baseMessage: String): Handle {
    override fun sendMessage(message: String) {
        println("CoffeeBot: $message")
    }

    override fun react(emoji: String) {
        println("Reacted $emoji to '$baseMessage'")
    }
}

class DiscordHandle(private val event: MessageCreateEvent): Handle {
    override fun sendMessage(message: String) {
        event.sendMessage(message)
    }

    override fun react(emoji: String) {
        event.message.addReaction(ReactionEmoji.unicode(emoji))
    }
}

interface MultiChannelHandle {
    fun sendChannelMessage(message: String, channelSnowflake: Snowflake)
    fun sendUserMessage(message: String, userSnowflake: Snowflake)
}

class StdoutMultiChannelHandle : MultiChannelHandle {
    override fun sendChannelMessage(message: String, channelSnowflake: Snowflake) {
        println("CoffeBot -> #$channelSnowflake: $message")
    }

    override fun sendUserMessage(message: String, userSnowflake: Snowflake) {
        println("CoffeBot -> @$userSnowflake: $message")
    }
}

class DiscordMultiChannelHandle(private val client: DiscordClient) : MultiChannelHandle {
    override fun sendChannelMessage(message: String, channelSnowflake: Snowflake) {
        client.getChannelById(channelSnowflake)
                .map { it as TextChannel }
                .subscribe { it.sendMessage(message) }
    }

    override fun sendUserMessage(message: String, userSnowflake: Snowflake) {
        client.getUserById(userSnowflake)
                .flatMap { it.privateChannel }
                .subscribe { it.sendMessage(message) }
    }
}
