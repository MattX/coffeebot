package coffeebot.message

import discord4j.core.`object`.util.Snowflake
import kotlin.math.min

fun loadMessage(user: User?, contents: String?, handle: Handle, userSnowflake: Snowflake?, channelSnowflake: Snowflake?): Message {
    return if (contents == null || user == null) {
        Invalid
    } else if (!contents.startsWith('!') || "CoffeeBot" == user.name) {
        Passive(user, contents, RepliableMessageHandle(handle))
    } else {
        Valid(user, contents, userSnowflake, channelSnowflake, RepliableMessageHandle(handle))
    }
}

interface RepliableMessage {
    fun reply(message: String)
    fun react(emoji: String)
}

class RepliableMessageHandle(private val handle: Handle) : RepliableMessage {
    companion object {
        private const val MAX_MESSAGE_SIZE = 2000
    }

    override fun reply(message: String) {
        handle.sendMessage(getPrefixToSend(message))
        if (message.length > MAX_MESSAGE_SIZE) {
            reply(message.substring(MAX_MESSAGE_SIZE))
        }
    }

    override fun react(emoji: String) {
        handle.react(emoji)
    }

    private fun getPrefixToSend(message: String): String {
        return message.substring(0, min(message.length, MAX_MESSAGE_SIZE))
    }
}

sealed class Message

data class Valid(val user: User,
                 val contents: String,
                 val userSnowflake: Snowflake?,
                 val channel: Snowflake?,
                 private val handle: RepliableMessage): Message(), RepliableMessage by handle {
    constructor(user: User, contents: String, handle: RepliableMessage) : this(user, contents, null, null , handle)
}

data class Passive(val user: User,
                   val contents: String,
                   private val handle: RepliableMessage):
        Message(), RepliableMessage by handle

object Invalid: Message()
