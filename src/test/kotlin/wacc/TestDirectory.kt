package wacc

import org.apache.commons.io.FilenameUtils
import picocli.CommandLine
import wacc.cli.Compile
import wacc.utils.Logging
import wacc.utils.logger
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class TestDirectory(path: String, private val returnCode: Int) : Logging {
    private val logger = logger()

    private val dirPath = Paths.get(path)
    private val programs = Files.walk(dirPath)
            .filter { Files.isRegularFile(it) }
            .filter { FilenameUtils.getExtension(it.toString()) == DEFAULT_EXTENSION }

    fun testPrograms() {
        programs.forEach {
            logger.info("Testing program $it")
            val exitCode = CommandLine(Compile()).execute(it.toString())
            logger.info("Got exit code $exitCode, expected code $returnCode")
            assertEquals(returnCode, exitCode)
        }
    }
}