package wacc.ast.codegen

import kotlin.math.max
import kotlin.math.min
import wacc.ast.AssignRhs
import wacc.ast.Expr
import wacc.ast.Stat


val Expr.weight: Int
    get() = when (this) {
        is Expr.Literal.IntLiteral -> 1
        is Expr.Literal.BoolLiteral -> 1
        is Expr.Literal.CharLiteral -> 1
        is Expr.Literal.StringLiteral -> 1
        is Expr.Literal.PairLiteral -> 1
        is Expr.Ident -> 1
        is Expr.ArrayElem -> (exprs.map {it.weight}.max() ?: 0) + 1
        is Expr.UnaryOp -> expr.weight
        is Expr.BinaryOp -> combineWeights(expr1.weight, expr2.weight)
    }

fun combineWeights(w1: Int, w2: Int): Int =
        min(max(w1, w2 + 1), max(w1 + 1, w2))
