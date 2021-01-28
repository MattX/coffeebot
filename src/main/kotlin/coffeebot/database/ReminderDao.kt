package coffeebot.database

import coffeebot.database.ReminderTable.channel
import coffeebot.database.ReminderTable.id
import coffeebot.database.ReminderTable.message
import coffeebot.database.ReminderTable.time
import coffeebot.database.ReminderTable.userName
import coffeebot.database.ReminderTable.userSnowflake
import discord4j.core.`object`.util.Snowflake
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneOffset

/**
 * Represents a reminder set for a certain time.
 *
 * @property channel can be null if the message should be sent directly to the user who created the reminder.
 */
data class Reminder(val userName: String,
                    val userSnowflake: Snowflake,
                    val time: Instant,
                    val message: String,
                    val channel: Snowflake?)

sealed class DeleteReminderResult
object Ok : DeleteReminderResult()
object NotFound : DeleteReminderResult()
object NotAllowed : DeleteReminderResult()

class ReminderDao {
    fun createReminder(reminder: Reminder): Int =
            ReminderTable.insertAndGetId {
                it[userName] = reminder.userName
                it[userSnowflake] = reminder.userSnowflake.asLong()
                it[time] = reminder.time.atZone(ZoneOffset.UTC).toLocalDateTime()
                it[message] = reminder.message
                it[channel] = reminder.channel?.asLong()
            }.value

    fun deleteReminder(id: Int, user: Snowflake): DeleteReminderResult = transaction {
        val reminders = ReminderTable.select { ReminderTable.id eq id }.toList()
        if (reminders.size != 1) {
            return@transaction NotFound
        }
        val reminder = reminders[0]
        if (Snowflake.of(reminder[ReminderTable.userSnowflake]) != user) {
            return@transaction NotAllowed
        }
        ReminderTable.deleteWhere { ReminderTable.id eq id }
        Ok
    }

    fun deleteReminder(id: Int): Boolean = ReminderTable.deleteWhere { ReminderTable.id eq id } == 1

    fun listReminders(): List<Pair<Int, Reminder>> =
            ReminderTable.selectAll().map {
                val id = it[id].value
                val reminder = Reminder(it[userName],
                        Snowflake.of(it[userSnowflake]),
                        it[time].toInstant(ZoneOffset.UTC),
                        it[message],
                        it[channel]?.let { Snowflake.of(it) })
                Pair(id, reminder)
            }
}
