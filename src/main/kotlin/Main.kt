import dao.Users
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.allowedMentions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

val usedTimeOuts = hashMapOf<Snowflake, Boolean>()

suspend fun main() {
    val timer = Timer("", true)

    // Connect do database
    val db = Database.connect(
        "jdbc:sqlite:/home/alex/Documents/Databases/discord.db",
        "org.sqlite.JDBC"
    )
    transaction {
        if (!Users.exists()) {
            SchemaUtils.create(Users)
        }
    }

    // Connect to discord API
    val token = {}.javaClass.getResource("token")?.readText()
        ?: error("Cannot read token file!")
    val kord = Kord(token)

    // Load weather state
    Weather.load()
    timer.schedule(TimeUnit.MINUTES.toMillis(5)) {
        Weather.load()
        runBlocking {
            kord.editPresence {
                if (Weather.weatherState == "bezchmurnie") {
                    playing("\uD83D\uDE03")
                } else {
                    playing("\uD83D\uDE21")
                }
            }
        }
    }

    // Create commands
    Command("ping") {
        it.messageCreateEvent.message.channel.createMessage("pong")
    }
    Command("najaktywniejsi") {
        var output = ""
        transaction {
            val result = Users.selectAll().limit(5).orderBy(Users.numberOfMessages, SortOrder.DESC).toList()
            result.forEach {
                runBlocking {
                    val user = kord.getUser(Snowflake(it[Users.id]))
                    output += user?.mention + ": " + it[Users.numberOfMessages] + '\n'
                }
            }
        }
        it.messageCreateEvent.message.channel.createMessage {
            content = output
            allowedMentions {
                users.clear()
            }
        }
    }
    Command("napisz") {
        var aktywny = false
        it.messageCreateEvent.member?.roles?.collect {role -> if (role.name == "Aktywny") aktywny = true }

        if (aktywny) {
            var content = ""
            for (argument in it.command.arguments) {
                content += "$argument "
            }
            it.messageCreateEvent.message.channel.createMessage {
                allowedMentions {
                    roles.clear()
                    users.clear()
                }
                this.content = content
            }
        }
    }.argumentRange = 1..100
    Command("pogoda") {
        it.messageCreateEvent.message.channel.createMessage(
            "Dzisiaj w Warszawie jest ${Weather.weatherState}! \n" +
                    "Temperatura wynosi ${Weather.temperature}°C!"
        )
    }
    Command("wycisz") {
        var contains = false
        it.messageCreateEvent.member?.roles?.collect {role -> if (role.name == "Aktywny") contains = true }

        // User has permissions to use the command
        if (contains) {
            if (usedTimeOuts[it.messageCreateEvent.message.author?.id] == true) {
                it.messageCreateEvent.message.channel.createMessage("Musisz poczekać 15 minut przed ponownym użyciem!")
                return@Command
            }
            val id = it.command.arguments[0].substring(2..(it.command.arguments[0].length-2))
            val user = kord.getUser(Snowflake(id))
            val member = it.messageCreateEvent.getGuildOrNull()?.getMemberOrNull(user!!.id)
            val roles = ArrayList(member?.roles?.toList()!!)
            for (role in member.roles.toList()) {
                member.removeRole(role.id)
            }
            member.addRole(Snowflake(1117478562522480660))
            usedTimeOuts[it.messageCreateEvent.message.author?.id!!] = true
            timer.schedule(TimeUnit.MINUTES.toMillis(15)) {
                usedTimeOuts[it.messageCreateEvent.message.author?.id!!] = false
            }
            timer.schedule(TimeUnit.MINUTES.toMillis(1)) {
                roles.forEach {
                    role ->
                    runBlocking {
                        member.addRole(role.id)
                    }
                }
                runBlocking {
                    member.removeRole(Snowflake(1117478562522480660))
                }
            }
            it.messageCreateEvent.message.channel.createMessage("Wyciszono ${user?.data?.username}")
        }
    }.argumentRange = 1..1

    // Start bot
    CommandExecutor.executeCommands(kord)
    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}