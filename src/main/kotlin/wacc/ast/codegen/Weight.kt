package wacc.ast.codegen

import kotlin.math.max
import kotlin.math.min
import wacc.ast.AssignRhs
import wacc.ast.Expr
import wacc.ast.Stat

val Stat.weight: Int
    get() = when (this) {
        is Stat.Skip -> 0 // TODO: is this 0 or 1?
        is Stat.AssignNew -> rhs.weight + 1
        is Stat.Assign -> rhs.weight // TODO: is this the case?
        is Stat.Read -> TODO()
        is Stat.Free -> expr.weight
        is Stat.Return -> expr.weight
        is Stat.Exit -> 0
        is Stat.Print -> expr.weight
        is Stat.Println -> expr.weight
        is Stat.IfThenElse -> listOf(expr.weight, branch1.weight, branch2.weight).max()!!
        is Stat.WhileDo -> TODO()
        is Stat.Begin -> stat.weight
        is Stat.Compose -> combineWeights(stat1.weight, stat2.weight)
    }

val AssignRhs.weight: Int
    get() = when (this) {
        is AssignRhs.Expression -> expr.weight
        is AssignRhs.ArrayLiteral -> TODO()
        is AssignRhs.Newpair -> TODO()
        is AssignRhs.PairElem -> TODO()
        is AssignRhs.Call -> TODO()
    }

val Expr.weight: Int
    get() = when (this) {
        is Expr.Literal.IntLiteral -> 1
        is Expr.Literal.BoolLiteral -> 1
        is Expr.Literal.CharLiteral -> 1
        is Expr.Literal.StringLiteral -> 1
        is Expr.Literal.PairLiteral -> TODO()
        is Expr.Ident -> TODO()
        is Expr.ArrayElem -> TODO()
        is Expr.UnaryOp -> expr.weight // TODO: consider the case for each operator
        is Expr.BinaryOp -> combineWeights(expr1.weight, expr2.weight)
    }

fun combineWeights(w1: Int, w2: Int): Int =
        min(max(w1, w2 + 1), max(w1 + 1, w2))
