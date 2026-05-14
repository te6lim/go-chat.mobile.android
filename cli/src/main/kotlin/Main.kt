import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import repl.CommandHandler
import repl.Printer

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val handler = CommandHandler(scope)

    Printer.info("GoChat CLI. Type 'help' for commands.")
    Printer.prompt()

    val stdin = System.`in`.bufferedReader()
    for (line in stdin.lines()) {
        if (line == null) break
        val trimmed = line.trim()
        if (trimmed.isEmpty()) { Printer.prompt(); continue }
        handler.handle(trimmed)
        if (trimmed == "exit" || trimmed == "quit") break
        Printer.prompt()
    }
}
