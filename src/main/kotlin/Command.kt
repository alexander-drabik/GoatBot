import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

class CommandInformation(val messageCreateEvent: MessageCreateEvent, val command: Command)

class Command(val name: String, val executes: suspend (CommandInformation) -> Unit = {}) {
    var arguments = mutableListOf<String>()
    var argumentRange = 0..0
    init {
        CommandExecutor.addCommand(this)
    }
}

object CommandExecutor {
    private var commands = hashMapOf<String, Command>()

    fun addCommand(command: Command) {
        commands[command.name] = command
    }

    fun executeCommands(kord: Kord) {
        kord.on<MessageCreateEvent> {
            // Do not respond to bots duh
            if (message.author?.isBot == true) return@on

            var content = message.content
            if (content[0] == '!') {
                // Remove prefix
                content = content.removePrefix("!")

                // Split string into words separated by spaces
                val words = content.split("\\s".toRegex())
                val name = words[0]
                val command = commands[name] ?: return@on

                // Push arguments to command
                command.arguments.clear()
                words.subList(1, words.size).forEach {
                    command.arguments.add(it)
                }

                // Check if number of arguments is valid
                if (words.size-1 in command.argumentRange) {
                    command.executes(CommandInformation(this, command))
                } else {
                    if (words.size-1 > command.argumentRange.last) {
                        this.message.channel.createMessage("Niepoprawna ilość argumentów, komenda przyjmuje maks. " +
                                command.argumentRange.last + when (command.argumentRange.last) {
                                    2, 3, 4 -> {
                                        " argumenty"
                                    }
                                    1 -> {
                                        " argument"
                                    }
                                    else -> {" argumentów"}
                                }
                            )
                    } else {
                        this.message.channel.createMessage("Niepoprawna ilość argumentów, komenda przyjmuje min. " +
                                command.argumentRange.first + " argument"
                        )
                    }
                }
            }
        }
    }
}