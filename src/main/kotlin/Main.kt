import dao.Users
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.create.allowedMentions
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun main() {
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

    // Create commands
    Command("ping") {
        it.messageCreateEvent.message.channel.createMessage("pong" + it.command.arguments.size)
    }.argumentRange = 0..1
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

    // Start bot
    CommandExecutor.executeCommands(kord)
    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}