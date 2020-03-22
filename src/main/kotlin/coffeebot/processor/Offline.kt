package coffeebot.processor

import coffeebot.commands.Dispatcher
import coffeebot.database.Database
import coffeebot.message.StdoutHandler
import coffeebot.message.User
import coffeebot.message.loadMessage

class Offline: MessageProcessor {
    private val dispatcher = Dispatcher(Database("offline_db.txt"))

    override fun run() {
        val syntax = "Syntax:\n\tUSERNAME COMMAND"

        println("Running offline mode!\n\n$syntax")
        var line: String?
        line = readLine()
        while (line != null) {
            try {
                val user = line.substringBefore(' ')
                val msg = line.substringAfter(' ')
                dispatcher.process(loadMessage(User(user), msg, StdoutHandler))
            } catch (e: Exception) {
                println("You caused an error: ${e.message}")
                println(syntax)
            }
            line = readLine()
        }
    }
}