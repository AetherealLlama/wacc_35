package wacc

import java.nio.file.Files
import java.nio.file.Paths
import org.apache.commons.io.FilenameUtils
import org.hamcrest.CoreMatchers.equalTo
import org.junit.rules.ErrorCollector
import picocli.CommandLine
import wacc.utils.Compile

class TestDirectory(path: String, private val returnCode: Int) {
    private val dirPath = Paths.get(path)
    private val programs = Files.walk(dirPath)
            .filter { Files.isRegularFile(it) }
            .filter { FilenameUtils.getExtension(it.toString()) == DEFAULT_EXTENSION }

    fun testPrograms(collector: ErrorCollector) {
        programs.forEach {
            println("Testing program $it")
            val exitCode = CommandLine(Compile()).execute(it.toString())
            if (exitCode != returnCode)
                println("Got exit code $exitCode, expected code $returnCode")
            else
                println("Got expected error code $exitCode")
            collector.checkThat(exitCode, equalTo(returnCode))
        }
    }
}
