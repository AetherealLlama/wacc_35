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
import wacc.ast.semantics.checkSemantics
import wacc.cli.visitors.ProgramVisitor
import wacc.utils.Logging
import wacc.utils.logger
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Callable

@Command(description = ["Compile a WACC program"], name = "wacc",
        mixinStandardHelpOptions = true, version = [wacc.VERSION])
class Compile : Callable<Int>, Logging {
    private val logger = logger()

    @Parameters(index="0", description = ["WACC program source to compile"])
    private var files: File? = null

    @Option(names = ["-s", "--semantic"], description = ["Perform semantic analysis"], negatable = true)
    private var semantic = true

    override fun call(): Int {
        val inputStream = FileInputStream(files!!)
        val charStream = CharStreams.fromStream(inputStream)
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())
        val tree = parser.program()

        if (parser.numberOfSyntaxErrors > 0) {
            println("${parser.numberOfSyntaxErrors} syntax errors. Halting compilation.")
            return RETURN_CODE_SYNTACTIC_ERROR
        } else if (semantic)
            return RETURN_CODE_OK

        val programVisitor = ProgramVisitor()
        val program = programVisitor.visit(tree)
        val errors = program.checkSemantics().reversed()
        errors.sorted().forEach(::println)
        if (errors.filter { !it.isSemantic }.isNotEmpty())
            return RETURN_CODE_SYNTACTIC_ERROR
        if (errors.filter { it.isSemantic }.isNotEmpty())
            return RETURN_CODE_SEMANTIC_ERROR
        return RETURN_CODE_OK
    }
}
