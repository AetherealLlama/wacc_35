package wacc

import kotlin.system.exitProcess
import org.koin.core.context.startKoin
import picocli.CommandLine
import wacc.utils.Compile
import wacc.utils.waccModule

fun main(args: Array<String>) {
    startKoin {
        modules(waccModule)
    }
    val exitCode = CommandLine(Compile()).execute(*args)
    exitProcess(exitCode)
}
