package wacc.ast

sealed class Expr {
    sealed class Literal : Expr() {
        data class IntLiteral(val value: Long) : Literal()
        data class BoolLiteral(val value: Boolean) : Literal()
        data class CharLiteral(val value: Char) : Literal()
        data class StringLiteral(val value: String) : Literal()
        object PairLiteral : Literal()
    }

    data class Ident(val name: String) : Expr()

    data class ArrayElem(val name: Ident, val exprs: Array<Expr>) : Expr() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ArrayElem

            if (name != other.name) return false
            if (!exprs.contentEquals(other.exprs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + exprs.contentHashCode()
            return result
        }
    }

    data class UnaryOp(val operator: UnaryOperator, val expr: Expr) : Expr()
    data class BinaryOp(val operator: BinaryOperator, val expr1: Expr, val expr2: Expr) : Expr()
}
