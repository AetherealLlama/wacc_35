package wacc

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector

class TestValidPrograms {
    @Rule
    @JvmField
    val collector = ErrorCollector()

    private val returnCode = RETURN_CODE_OK
    private val basePath = "wacc_examples/valid/"
    private val directories = arrayOf(
            TestDirectory(basePath + "advanced", returnCode),
            TestDirectory(basePath + "array", returnCode),
            TestDirectory(basePath + "basic", returnCode),
            TestDirectory(basePath + "expressions", returnCode),
            TestDirectory(basePath + "function", returnCode),
            TestDirectory(basePath + "if", returnCode),
            TestDirectory(basePath + "IO", returnCode),
            TestDirectory(basePath + "pairs", returnCode),
            TestDirectory(basePath + "runtimeErr", returnCode),
            TestDirectory(basePath + "scope", returnCode),
            TestDirectory(basePath + "sequence", returnCode),
            TestDirectory(basePath + "variables", returnCode),
            TestDirectory(basePath + "while", returnCode)
    )

    @Test
    fun runTests() {
        directories.forEach { it.testPrograms(collector) }
    }
}
