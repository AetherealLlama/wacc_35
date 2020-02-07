package wacc

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector

class TestInvalidSyntacticPrograms {
    @Rule
    @JvmField
    val collector = ErrorCollector()

    private val returnCode = RETURN_CODE_SYNTACTIC_ERROR
    private val basePath = "wacc_examples/invalid/syntaxErr/"
    private val directories = arrayOf(
            TestDirectory(basePath + "array", returnCode),
            TestDirectory(basePath + "basic", returnCode),
            TestDirectory(basePath + "expressions", returnCode),
            TestDirectory(basePath + "function", returnCode),
            TestDirectory(basePath + "if", returnCode),
            TestDirectory(basePath + "pairs", returnCode),
            TestDirectory(basePath + "print", returnCode),
            TestDirectory(basePath + "sequence", returnCode),
            TestDirectory(basePath + "variables", returnCode),
            TestDirectory(basePath + "while", returnCode)
    )

    @Test
    fun runTests() {
        directories.forEach { it.testPrograms(collector) }
    }
}
