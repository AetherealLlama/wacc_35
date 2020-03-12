package wacc

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import wacc.utils.waccModule

class TestInvalidSemanticPrograms {
    @Rule
    @JvmField
    val collector = ErrorCollector()

    private val returnCode = RETURN_CODE_SEMANTIC_ERROR
    private val basePath = "wacc_examples/invalid/semanticErr/"
    private val directories = arrayOf(
            TestDirectory(basePath + "exit", returnCode),
            TestDirectory(basePath + "expressions", returnCode),
            TestDirectory(basePath + "function", returnCode),
            TestDirectory(basePath + "if", returnCode),
            TestDirectory(basePath + "IO", returnCode),
            TestDirectory(basePath + "multiple", returnCode),
            TestDirectory(basePath + "pairs", returnCode),
            TestDirectory(basePath + "print", returnCode),
            TestDirectory(basePath + "read", returnCode),
            TestDirectory(basePath + "scope", returnCode),
            TestDirectory(basePath + "variables", returnCode),
            TestDirectory(basePath + "while", returnCode),
            TestDirectory(basePath + "exit", returnCode)
    )

    @Test
    fun runTests() {
        startKoin { modules(waccModule) }
        directories.forEach { it.testPrograms(collector) }
        stopKoin()
    }
}
