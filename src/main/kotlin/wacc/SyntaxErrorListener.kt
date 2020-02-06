package wacc

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import wacc.utils.Logging
import wacc.utils.getClassForLogging
import wacc.utils.logger

class SyntaxErrorListener() : BaseErrorListener(), Logging {
    override fun syntaxError(recognizer: Recognizer<*, *>?,
                             offendingSymbol: Any?,
                             line: Int,
                             charPositionInLine: Int,
                             msg: String?,
                             e: RecognitionException?) {
        this.logger().error("Syntax error at line $line:$charPositionInLine : $msg")
    }
}