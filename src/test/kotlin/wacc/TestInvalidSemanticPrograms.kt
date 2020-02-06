package wacc

import org.junit.Test

class TestInvalidSemanticPrograms {
    private val returnCode = 200
    private val basePath = "wacc_examples/invalid/semanticErr"
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
        directories.forEach(TestDirectory::testPrograms)
    }
}
