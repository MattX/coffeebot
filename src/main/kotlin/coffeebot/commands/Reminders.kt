package coffeebot.commands

import coffeebot.database.*
import coffeebot.message.MultiChannelHandle
import discord4j.core.`object`.util.Snowflake
import org.ocpsoft.prettytime.nlp.PrettyTimeParser
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val dateParser = PrettyTimeParser()
private val scheduleRegex = Regex("!remindme\\s+(?<time>[^:]+)\\s*(:\\s+(?<message>.*))?")
private val cancelRegex = Regex("!cancel-reminder\\s+(?<id>[0-9]+)")

class ReminderManager(private val reminderDao: ReminderDao, private val handle: MultiChannelHandle) {
    private data class InMemoryTask(val reminder: Reminder, val task: ScheduledFuture<*>)

    private val executor = ScheduledThreadPoolExecutor(1)
    private val tasks = mutableMapOf<Int, InMemoryTask>()

    private fun loadReminders() {
        for ((id, reminder) in reminderDao.listReminders()) {
            schedule(reminder, id)
        }
    }

    private fun addReminder(reminder: Reminder): Int {
        val id = reminderDao.createReminder(reminder)
        schedule(reminder, id)
        return id
    }

    private fun schedule(reminder: Reminder, id: Int) {
        val delay = Duration.between(Instant.now(), reminder.time).coerceAtLeast(Duration.ZERO)
        val task = executor.schedule({ doReminder(reminder, id) }, delay.toMillis(), TimeUnit.MILLISECONDS)
        tasks[id] = InMemoryTask(reminder, task)
    }

    private fun doReminder(reminder: Reminder, id: Int) {
        if (reminder.channel != null) {
            handle.sendChannelMessage(reminder.message, reminder.channel)
        } else {
            handle.sendUserMessage(reminder.message, reminder.userSnowflake)
        }
        reminderDao.deleteReminder(id)
    }

    private fun deleteReminder(id: Int, user: Snowflake): DeleteReminderResult {
        val task = tasks[id] ?: return NotFound
        if (task.reminder.userSnowflake != user) {
            return NotAllowed
        }
        task.task.cancel(false)
        tasks.remove(id)
        reminderDao.deleteReminder(id)
        return Ok
    }

    val remindme = Command("!remindme", "Schedule a message to be sent at a later time") { message ->
        if (message.userSnowflake == null) {
            message.reply("Need a valid user snowflake to schedule a reminder.")
            return@Command
        }
        val matched = scheduleRegex.matchEntire(message.contents)
        println(message.contents)
        if (matched == null) {
            message.reply("Invalid syntax. Try `!remindme TIME(: MESSAGE)`.")
            return@Command
        }
        val dateString = matched.groups["time"]!!.value
        val dates = dateParser.parse(dateString)
        if (dates.size != 1) {
            message.reply("I got confused trying to parse the date out of your message :/")
            return@Command
        }
        val instant = dates[0].toInstant()
        // TODO this @${message.user} business is sensitive to name collisions, but we need a Client to get the
        //      always-correct version.
        val reminderMessage = matched.groups["message"]?.value ?: "Hey @${message.user}, this is your reminder!"
        val reminder = Reminder(message.user.name, message.userSnowflake, instant, reminderMessage, message.channel)
        val id = addReminder(reminder)
        message.reply("Ok, I'll remind you at $instant. If you want to cancel this reminder, type `!cancel-reminder $id`.")
    }

    val cancelReminder = Command("!cancel-reminder", "Cancel a reminder") { message ->
        if (message.userSnowflake == null) {
            message.reply("Need a valid user snowflake to cancel a reminder.")
            return@Command
        }
        val matched = cancelRegex.matchEntire(message.contents)
        if (matched == null) {
            message.reply("Invalid syntax. Try `!cancel-reminder ID`.")
            return@Command
        }
        val id = matched.groups["id"]!!.value.toInt()
        when (deleteReminder(id, message.userSnowflake)) {
            Ok -> message.reply("Reminder $id deleted.")
            NotFound -> message.reply("$id is not a known reminder.")
            NotAllowed -> message.reply("You are not the creator of reminder $id.")
        }
    }
}
