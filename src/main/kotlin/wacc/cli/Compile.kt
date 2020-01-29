package wacc.cli

import picocli.CommandLine.*
import wacc.utils.Logging
import wacc.utils.logger
import java.io.File
import java.util.concurrent.Callable

@Command(description = ["Compile a WACC program"], name = "wacc",
        mixinStandardHelpOptions = true, version = [wacc.VERSION])
class Compile : Callable<Int>, Logging {
    private val logger = logger()

    @Parameters(index = "0", description = ["WACC program to compile"])
    private var file: File? = null

    @Option(names = ["-d", "--debug"], description = ["Print debug information"])
    private var debug = false

    override fun call(): Int {
        logger.info("Debug mode is ${if (debug) "on" else "off"}")
        logger.error("Not yet implemented")
        return 0
    }
}