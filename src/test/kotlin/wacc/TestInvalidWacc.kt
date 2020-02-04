package wacc

import org.apache.commons.io.FilenameUtils
import org.junit.Test
import picocli.CommandLine
import wacc.cli.Compile
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class TestInvalidWacc {
    private val enabledSemanticTests = false
    private val enabledSyntacticTests = false

    private val expectedSemanticCode = 200
    private val expectedSyntacticCode = 100
    private val semanticErr = Paths.get("wacc_examples/invalid/semanticErr")
    private val syntacticErr = Paths.get("wacc_examples/invalid/syntaxErr")

    private val semanticPrograms = Files.walk(semanticErr)
            .filter { Files.isRegularFile(it) }
            .filter { FilenameUtils.getExtension(it.toString()) == "wacc" }

    private val syntacticPrograms = Files.walk(syntacticErr)
            .filter { Files.isRegularFile(it) }
            .filter { FilenameUtils.getExtension(it.toString()) == "wacc" }

    @Test
    fun testSemanticInvalid() {
        if (enabledSemanticTests) {
            semanticPrograms.forEach {
                println("Testing program $it")
                val exitCode = CommandLine(Compile()).execute(it.toString())
                println("Got exit code $exitCode")
                assertEquals(expectedSemanticCode, exitCode)
            }
        }
    }

    @Test
    fun testSyntacticInvalid() {
        if (enabledSyntacticTests) {
            syntacticPrograms.forEach {
                println("Testing program $it")
                val exitCode = CommandLine(Compile()).execute(it.toString())
                println("Got exit code $exitCode")
                assertEquals(expectedSyntacticCode, exitCode)
            }
        }
    }
}
