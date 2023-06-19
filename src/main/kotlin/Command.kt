import dao.Users
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.random.Random
import kotlin.random.nextInt

class CommandInformation(val messageCreateEvent: MessageCreateEvent, val command: Command)

class Command(val name: String, val executes: suspend (CommandInformation) -> Unit = {}) {
    var arguments = mutableListOf<String>()
    var argumentRange = 0..0
    init {
        CommandExecutor.addCommand(this)
    }
}

val numberOfMessages = hashMapOf<Snowflake, Int>()
val timerCommand = Timer("", true)
object CommandExecutor {
    private var commands = hashMapOf<String, Command>()

    fun addCommand(command: Command) {
        commands[command.name] = command
    }

    fun executeCommands(kord: Kord) {
        kord.on<MessageCreateEvent> {
            // Do not respond to bots duh
            if (message.author?.isBot == true) return@on

            // Anti spam
            if (numberOfMessages.containsKey(message.author?.id)) {
                numberOfMessages[message.author?.id!!] = numberOfMessages[message.author?.id]!! + 1
                if (numberOfMessages[message.author?.id!!]!! > 10) {
                    message.channel.createMessage(message.author?.mention + " zamknij się")
                    val member = getGuildOrNull()?.getMemberOrNull(message.author!!.id)
                    member?.addRole(Snowflake(1117478562522480660))
                }
            } else {
                numberOfMessages[message.author?.id!!] = 0
                val member = getGuildOrNull()?.getMemberOrNull(message.author!!.id)
                timerCommand.schedule(TimeUnit.MINUTES.toMillis(1)) {
                    numberOfMessages.remove(message.author?.id)
                    runBlocking {
                        member?.removeRole(Snowflake(1117478562522480660))
                    }
                }
            }

            // Co
            if (message.content == "co" || message.content == "co?") {
                if ((0..5).shuffled().last() == 0) {
                    message.channel.createMessage(message.author?.mention + " gówno")
                }
            }

            // Anti sex
            if (message.content.contains("seks")
                || message.content.contains("sex")
                || message.content.contains("cycki")
                || message.content.contains("ruchanie")
                || message.content.contains("ruchać")
                ) {
                message.channel.createMessage(message.author?.mention + " ZBOCZENIEC!")
            }

            if (message.mentionedUsers.toList().isNotEmpty() && message.mentionedUsers.first().isBot) {
                val send = when ((0..5).shuffled().last()) {
                    1 -> "hejka :D"
                    2 -> "co tam?"
                    3 -> "kocham trawę"
                    4 -> "subskrybuj goatcode"
                    else -> "aaaaaaa"
                }
                message.channel.createMessage(message.author?.mention + " $send")
            }

            if (message.content.length > 500) {
                val send = when ((0..5).shuffled().last()) {
                    1 -> "myślisz że ktoś to przeczyta?"
                    2 -> "cichaj nerdzie"
                    3 -> "nikt cie nie lubi"
                    4 -> "uspokój się"
                    else -> "zamknij sie"
                }
                message.channel.createMessage(message.author?.mention + " " + send)

                return@on
            }

            // Increment message count
            if (message.channelId != Snowflake(1117139253479080027)) {
                transaction {
                    val result = Users.select {
                        Users.id eq message.author?.id.toString()
                    }
                    val user: ResultRow? = result.toList().firstOrNull()
                    if (user == null) {
                        Users.insert {
                            it[id] = message.author?.id.toString()
                            it[numberOfMessages] = 1
                        }
                    } else {
                        Users.update({ Users.id eq message.author?.id.toString() }) {
                            it[numberOfMessages] = user[Users.numberOfMessages]+1
                        }
                    }
                }
            }

            var content = message.content
            print(message.author?.data?.username + " ")
            println(content)
            if (content[0] == '!') {
                // Random chance to not execute command
                if (Random.nextInt(0..50) == 1) {
                    val send = when ((0..5).shuffled().last()) {
                        1 -> "sry nie chcę mi się"
                        2 -> "może potem"
                        3 -> "daj mi spokój"
                        4 -> "jeeezu, co ja jestem, twoja służba?"
                        else -> "gram w minecraft nie mogę"
                    }
                    message.channel.createMessage(message.author?.mention + " " + send)

                    return@on
                }

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
                        this.message.channel.createMessage(
                            "Niepoprawna ilość argumentów, komenda przyjmuje maks. " +
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
                        this.message.channel.createMessage(
                            "Niepoprawna ilość argumentów, komenda przyjmuje min. " +
                                    command.argumentRange.first + when (command.argumentRange.first) {
                                2, 3, 4 -> {
                                    " argumenty"
                                }
                                1 -> {
                                    " argument"
                                }
                                else -> {" argumentów"}
                            }
                        )
                    }
                }
            }
        }
    }
}