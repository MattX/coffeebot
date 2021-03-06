package coffeebot.commands

import coffeebot.database.Log
import coffeebot.database.ReminderDao
import coffeebot.message.*

// Each Discord server should probably get its own Dispatcher so it has its own state
class Dispatcher(private val log: Log?, miltonSecret: String?, multiChannelHandle: MultiChannelHandle) {

    private val registered = mutableListOf<Command>()
    private val miltonCommand: PassiveCommand? = if (miltonSecret != null) {
        MiltonClient(miltonSecret).command
    } else {
        println("No milton secret passed, not starting Milton client")
        null
    }
    private val passiveRegistered = listOfNotNull(miltonCommand)

    // TODO: Figure out how to represent help. This feels sloppy
    private val help = Command("!help", "Invokes help") {
        val helpString = registered.joinToString("\n\t") { command -> command.printHelp() }
        it.reply("Commands:\n\t$helpString")
    }

    private val reminderManager = ReminderManager(ReminderDao(), multiChannelHandle)

    init {
        this.register(ping)
                .register(bet)
                .register(accept)
                .register(cancel)
                .register(adjudicate)
                .register(list)
                .register(lisp)
                .register(pay)
                .register(source)
                .register(totals)
                .register(help)
                .register(reminderManager.remindme)
                .register(reminderManager.cancelReminder)
                .register(reminderManager.listReminders)
        this.loadFromLog()
    }

    fun process(message: Message) {
        when (message) {
            is Valid -> {
                log?.commit(message)
                dispatch(message)
            }
            is Passive -> {
                dispatchPassive(message)
            }
            is Invalid -> {}
        }
    }

    private fun register(command: Command): Dispatcher {
        registered.add(command)
        return this
    }

    private fun loadFromLog() {
        log?.loadMessagesFromLog()?.forEach {
            if (it.contents.startsWith("!cl")) {
                println("[Lisp] Applying lisp: $it")
                dispatch(it)
            } else {
                println("[Lisp] Ignoring message: $it")
            }
        }
    }

    private fun dispatch(message: Valid) {
        val command = this.registered.firstOrNull { it.matches(message) }
        if (command != null) {
            try {
                command.handle(message)
            } catch (e: Exception) {
                println("Caught Exception processing $message using $command: $e")
            }
        } else {
            invalid.handle(message)
        }
    }

    private fun dispatchPassive(message: Passive) {
        passiveRegistered.forEach { it.handle(message) }
    }
}
