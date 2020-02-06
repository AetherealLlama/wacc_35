package wacc.cli

import WaccLexer
import WaccParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import picocli.CommandLine.*
import wacc.VERSION
import wacc.ast.semantics.checkSemantics
import wacc.cli.visitors.ProgramVisitor
import wacc.utils.Logging
import wacc.utils.logger
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Callable

@Command(description = ["Compile a WACC program"], name = "wacc",
        mixinStandardHelpOptions = true, version = [VERSION])
class Compile : Callable<Int>, Logging {
    private val logger = logger()

    @Parameters(arity = "1..*", description = ["WACC program(s) to compile"])
    private var files: Array<File>? = null

    @Option(names = ["-d", "--debug"], description = ["Print debug information"])
    private var debug = false

    override fun call(): Int {
        logger.info("Debug mode is ${if (debug) "on" else "off"}")
        val inputStream = FileInputStream(files!![0])
        val charStream = CharStreams.fromStream(inputStream)
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)
        val tree = parser.program()
        val programVisitor = ProgramVisitor()
        val program = programVisitor.visit(tree)
        val errors = program.checkSemantics().reversed()
        errors.forEach(::println)
        return 0
    }
}