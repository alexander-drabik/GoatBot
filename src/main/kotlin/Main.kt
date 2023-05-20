import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent

suspend fun main() {
    val token = {}.javaClass.getResource("token")?.readText()
        ?: error("Cannot read token file!")

    val kord = Kord(token)

    Command("ping") {
        it.messageCreateEvent.message.channel.createMessage("pong" + it.command.arguments.size)
    }.argumentRange = 0..1

    CommandExecutor.executeCommands(kord)

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}