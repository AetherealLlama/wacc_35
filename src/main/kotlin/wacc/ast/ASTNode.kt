package wacc.ast

import org.antlr.v4.runtime.ParserRuleContext

abstract class ASTNode(val pos: FilePos)

data class FilePos(val line: Int, val posInLine: Int)

val ParserRuleContext.pos: FilePos
    get() = FilePos(start.line, start.charPositionInLine)