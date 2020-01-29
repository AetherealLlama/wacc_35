package wacc

import picocli.CommandLine
import wacc.cli.Compile
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandLine(Compile()).execute(*args)
    exitProcess(exitCode)
}