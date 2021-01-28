package coffeebot.message

import discord4j.core.`object`.util.Snowflake

data class User(val name: String, val snowflake: Snowflake?) {
    constructor(name: String) : this(name, null)

    override fun toString(): String {
        return this.name
    }
}
