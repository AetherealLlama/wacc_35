package wacc.cli

import WaccLexer
import WaccParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import picocli.CommandLine.*
import wacc.RETURN_CODE_OK
import wacc.RETURN_CODE_SEMANTIC_ERROR
import wacc.RETURN_CODE_SYNTACTIC_ERROR
import wacc.SyntaxErrorListener
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
        // Generate input from file
        val inputStream = FileInputStream(files!![0])
        val charStream = CharStreams.fromStream(inputStream)

        // Lex and parse the input
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)

        // Replace error listener with our logging one
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())

        val tree = parser.program()

        // Check for syntax errors from the parser
        if (parser.numberOfSyntaxErrors > 0) {
            println("${parser.numberOfSyntaxErrors} syntax errors. Halting compilation.")
            return RETURN_CODE_SYNTACTIC_ERROR
        }

        // Generate the AST
        val programVisitor = ProgramVisitor()
        val program = programVisitor.visit(tree)

        // Check for further syntax and semantic errors from the tre
        val errors = program.checkSemantics().reversed()
        errors.sorted().forEach(::println)
        if (errors.filter { !it.isSemantic }.isNotEmpty())
            return RETURN_CODE_SYNTACTIC_ERROR
        if (errors.filter { it.isSemantic }.isNotEmpty())
            return RETURN_CODE_SEMANTIC_ERROR
        return RETURN_CODE_OK
    }
}
