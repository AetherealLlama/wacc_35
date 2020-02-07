package wacc.ast

import org.antlr.v4.runtime.ParserRuleContext

abstract class ASTNode(val pos: FilePos)

data class FilePos(val line: Int, val posInLine: Int) : Comparable<FilePos> {
    override fun compareTo(other: FilePos): Int =
            line.compareTo(other.line).let { if (it == 0) posInLine.compareTo(other.posInLine) else it }
}

val ParserRuleContext.pos: FilePos
    get() = FilePos(start.line, start.charPositionInLine)
