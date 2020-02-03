package wacc.ast

sealed class Expr {
    sealed class Literal : Expr() {
        data class IntLiteral(val value: Int) : Literal()
        data class BoolLiteral(val value: Boolean) : Literal()
        data class CharLiteral(val value: Char) : Literal()
        data class StringLiteral(val value: String) : Literal()
        object PairLiteral : Expr()
    }

    data class Ident(val name: String) : Expr()

    data class ArrayElem(val name: Ident, val expr: Expr) : Expr()

    data class UnaryOp(val operator: UnaryOperator, val expr: Expr) : Expr()
    data class BinaryOp(val operator: BinaryOperator, val expr1: Expr, val expr2: Expr) : Expr()
}
