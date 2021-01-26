package coffeebot.commands

import org.ocpsoft.prettytime.nlp.PrettyTimeParser
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val executor = ScheduledThreadPoolExecutor(1)
private val dateParser = PrettyTimeParser()
private val scheduleRegex = Regex("!remindme\\s+(?<time>[^:]+)\\s*(:\\s+(?<message>.*))?")

val remindme = Command("!remindme", "Schedule a message to be sent at a later time") { message ->
    val matched = scheduleRegex.matchEntire(message.contents)
    println(message.contents)
    if (matched == null) {
        message.reply("Apparently I can't parse your message")
        return@Command
    }
    val dateString = matched.groups["time"]!!.value
    val dates = dateParser.parse(dateString)
    if (dates.size != 1) {
        message.reply("I got confused trying to parse the date out of your message :/")
        return@Command
    }
    val delayMillis = dates[0].time - Date().time
    val reminderMessage = matched.groups["message"]?.value ?: "Hey @${message.user}, this is your reminder!"
    executor.schedule({ message.reply(reminderMessage) }, delayMillis, TimeUnit.MILLISECONDS)
    message.reply("Ok, I'll remind you at ${dates[0]}")
}
