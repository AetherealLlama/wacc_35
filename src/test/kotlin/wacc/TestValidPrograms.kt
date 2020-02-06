package wacc

import org.junit.Test

class TestValidPrograms {
    private val returnCode = 0
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
        directories.forEach(TestDirectory::testPrograms)
    }
}
