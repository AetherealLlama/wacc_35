package wacc

import org.apache.commons.io.FilenameUtils
import org.junit.Test
import picocli.CommandLine
import wacc.cli.Compile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals

class TestValidWacc {
    private val enabledValidTests = true

    private val expectedCode = 0
    private val path = Paths.get("wacc_examples/valid")

    private val programs = Files.walk(path)
            .filter { Files.isRegularFile(it) }
            .filter { FilenameUtils.getExtension(it.toString()) == "wacc" }

    @Test
    fun testValid() {
        if (enabledValidTests) {
            programs.forEach { path: Path? ->
                println("Testing program $path")
                val exitCode = CommandLine(Compile()).execute(path.toString())
                println("Got exit code $exitCode")
                assertEquals(expectedCode, exitCode)
            }
        }
    }
}
