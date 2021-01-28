package coffeebot.database

import coffeebot.database.ReminderTable.message
import coffeebot.database.ReminderTable.time
import coffeebot.database.ReminderTable.user
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneOffset

data class Reminder(val user: String, val time: Instant, val message: String)

sealed class DeleteReminderResult
object Ok : DeleteReminderResult()
object NotFound : DeleteReminderResult()
object NotAllowed : DeleteReminderResult()

class ReminderDao {
    fun createReminder(reminder: Reminder): Int =
            ReminderTable.insertAndGetId {
                it[user] = reminder.user
                it[time] = reminder.time.atZone(ZoneOffset.UTC).toLocalDateTime()
                it[message] = reminder.message
            }.value

    fun deleteReminder(id: Int, user: String): DeleteReminderResult = transaction {
        val reminders = ReminderTable.select { ReminderTable.id eq id }.toList()
        if (reminders.size != 1) {
            return@transaction NotFound
        }
        val reminder = reminders[0]
        if (reminder[ReminderTable.user] != user) {
            return@transaction NotAllowed
        }
        ReminderTable.deleteWhere { ReminderTable.id eq id }
        Ok
    }

    fun deleteReminder(id: Int): Boolean = ReminderTable.deleteWhere { ReminderTable.id eq id } == 1

    fun listReminders(): List<Reminder> =
            ReminderTable.selectAll().map {
                Reminder(it[user], it[time].toInstant(ZoneOffset.UTC), it[message])
            }
}
