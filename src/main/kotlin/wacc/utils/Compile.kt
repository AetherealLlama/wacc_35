package wacc.utils

import WaccLexer
import WaccParser
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.koin.core.KoinComponent
import org.koin.core.inject
import picocli.CommandLine.*
import wacc.RETURN_CODE_OK
import wacc.RETURN_CODE_SEMANTIC_ERROR
import wacc.RETURN_CODE_SYNTACTIC_ERROR
import wacc.ast.Include
import wacc.ast.Program
import wacc.ast.visitors.LibraryVisitor
import wacc.ast.visitors.ProgramVisitor
import wacc.checker.SyntaxErrorListener
import wacc.checker.checkSemantics
import wacc.codegen.getAsm

lateinit var executor: ExecutorService

fun <T> submit(f: () -> T): Future<T> = executor.submit(Callable(f))

private val Include.library: Pair<Int, Program?>
    get() {
        val file = File(filename)
        val inputStream = FileInputStream(file)
        val charStream = CharStreams.fromStream(inputStream)

        // Lex and parse the input
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)

        // Replace error listener with our logging one
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())

        val tree = parser.library()

        if (parser.numberOfSyntaxErrors > 0) {
            println("${parser.numberOfSyntaxErrors} syntax errors in $filename. Halting compilation.")
            return RETURN_CODE_SYNTACTIC_ERROR to null
        }

        val libraryVisitor = LibraryVisitor()
        val library = libraryVisitor.visit(tree)

        val (code, fullLib) = library.fullProgram
        if (code != RETURN_CODE_OK)
            return code to null

        val errors = fullLib!!.checkSemantics().reversed()
        errors.sorted().forEach { println("$filename: $it") }
        if (errors.any { !it.isSemantic })
            return RETURN_CODE_SYNTACTIC_ERROR to null
        if (errors.any { it.isSemantic })
            return RETURN_CODE_SEMANTIC_ERROR to null

        return RETURN_CODE_OK to library
    }

private infix fun Program.including(library: Program): Program =
        Program(null, library.classes + classes, library.funcs + funcs, stat)

private val Program.fullProgram: Pair<Int, Program?>
    get() =
        if (includes!!.isEmpty()) {
            RETURN_CODE_OK to this
        } else {
            includes.map { submit {
                val (code, library) = it.library
                if (code != RETURN_CODE_OK)
                    code to null
                else
                    library!!.fullProgram
            } }.fold(RETURN_CODE_OK to this as Program?) { (progCode, program), libFuture ->
                val (libCode, library) = libFuture.get()
                when {
                    progCode != RETURN_CODE_OK -> progCode to null as Program?
                    libCode != RETURN_CODE_OK -> libCode to null as Program?
                    else -> RETURN_CODE_OK to (program!! including library!!)
                }
            }
        }

@Command(description = ["Compile a WACC program"], name = "wacc",
        mixinStandardHelpOptions = true, version = [wacc.VERSION])
class Compile : Callable<Int>, KoinComponent, Logging {
    private val logger = logger()
    private val start = Instant.now()
    private val programVisitor: ProgramVisitor by inject()

    @Parameters(index = "0", description = ["WACC program source to compile"])
    private var file: File? = null

    @Option(names = ["-s", "--semantic"], description = ["Perform semantic analysis"], negatable = true)
    private var semantic = true

    @Option(names = ["-t", "--stdout"], description = ["Print output to STDOUT"])
    private var stdout = false

    @Option(names = ["-m", "--measure"], description = ["Print compilation time"])
    private var measure = false

    @Option(names = ["-j", "--threads"], description = ["The maximum number of threads"])
    private var threadCount = Runtime.getRuntime().availableProcessors()

    override fun call(): Int {
        executor = Executors.newWorkStealingPool(threadCount)

        // Generate input from file
        val inputStream = FileInputStream(file!!)
        val charStream = CharStreams.fromStream(inputStream)

        // Lex and parse the input
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)

        // Replace error listener with our logging one
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())

        val tree = parser.program()

        // Check for syntax errors from
        if (parser.numberOfSyntaxErrors > 0) {
            println("${parser.numberOfSyntaxErrors} syntax errors. Halting compilation.")
            printTimeToRun()
            return RETURN_CODE_SYNTACTIC_ERROR
        } else if (!semantic) {
            printTimeToRun()
            return RETURN_CODE_OK
        }

        // Generate the AST
        val program = programVisitor.visit(tree)

        val (code, fullProgram) = program.fullProgram
        if (code != RETURN_CODE_OK) {
            printTimeToRun()
            return code
        }

        // Check for further syntax and semantic errors from the tree
        val errors = fullProgram!!.checkSemantics().reversed()
        errors.sorted().forEach { println("${file!!.name}: $it") }
        if (errors.any { !it.isSemantic }) {
            printTimeToRun()
            return RETURN_CODE_SYNTACTIC_ERROR
        }
        if (errors.any { it.isSemantic }) {
            printTimeToRun()
            return RETURN_CODE_SEMANTIC_ERROR
        }

        val programAsm = fullProgram.getAsm()
        if (stdout) {
            println(programAsm)
        } else {
            val asmFile = File(file!!.nameWithoutExtension + ".s")
            asmFile.writeText(programAsm)
        }

        printTimeToRun()
        return RETURN_CODE_OK
    }

    private fun printTimeToRun() {
        if (measure) {
            val finish = Instant.now()
            val timeElapsed = Duration.between(start, finish).toMillis()
            println("Compilation took ${timeElapsed}ms.")
        }
    }
}
