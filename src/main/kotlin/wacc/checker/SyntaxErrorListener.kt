package wacc.checker

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import wacc.utils.Logging

// New ANTLR Error listener making use of the logger
class SyntaxErrorListener() : BaseErrorListener(), Logging {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        println("Syntax error at line $line:$charPositionInLine : $msg")
    }
}
