package wacc

import kotlin.system.exitProcess
import picocli.CommandLine
import wacc.utils.Compile

fun main(args: Array<String>) {
    val exitCode = CommandLine(Compile()).execute(*args)
    exitProcess(exitCode)
}
