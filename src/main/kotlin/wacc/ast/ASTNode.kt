package wacc.ast

import org.antlr.v4.runtime.ParserRuleContext

/**
 * The base AST Node
 *
 * Contains no information other than its position, is abstract
 *
 * @property pos
 */
abstract class ASTNode(val pos: FilePos)

/**
 * File position, representing the line number and position in line used by all nodes
 */
data class FilePos(val line: Int, val posInLine: Int) : Comparable<FilePos> {
    override fun compareTo(other: FilePos): Int =
            line.compareTo(other.line).let { if (it == 0) posInLine.compareTo(other.posInLine) else it }
}

val ParserRuleContext.pos: FilePos
    get() = FilePos(start.line, start.charPositionInLine)
