package wacc.ast

sealed class Stat {
    object Skip : Stat()
    data class AssignNew(val type: Type, val name: String, val rhs: AssignRhs) : Stat()
    data class Assign(val lhs: AssignLhs, val rhs: AssignRhs) : Stat()
    data class Read(val lhs: AssignLhs) : Stat()
    data class Free(val expr: Expr) : Stat()
    data class Return(val expr: Expr) : Stat()
    data class Exit(val expr: Expr) : Stat()
    data class Print(val expr: Expr) : Stat()
    data class Println(val expr: Expr) : Stat()
    data class IfThenElse(val expr: Expr, val branch1: Stat, val branch2: Stat) : Stat()
    data class WhileDo(val expr: Expr, val stat: Stat) : Stat()
    data class Begin(val stat: Stat) : Stat()
    data class Compose(val stat1: Stat, val stat2: Stat) : Stat()
}

sealed class AssignLhs {
    data class Variable(val name: String) : AssignLhs()
    data class ArrayElem(val name: String, val exprs: Array<Expr>) : AssignLhs() {
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

    data class PairElem(val accessor: PairAccessor, val expr: Expr) : AssignLhs()
}

sealed class AssignRhs {
    data class Expression(val expr: Expr) : AssignRhs()
    data class ArrayLiteral(val exprs: Array<Expr>) : AssignRhs() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ArrayLiteral

            if (!exprs.contentEquals(other.exprs)) return false

            return true
        }

        override fun hashCode(): Int {
            return exprs.contentHashCode()
        }
    }

    data class Newpair(val expr1: Expr, val expr2: Expr) : AssignRhs()
    data class PairElem(val accessor: PairAccessor, val expr: Expr) : AssignRhs()
    data class Call(val name: String, val args: Array<Expr>) : AssignRhs() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Call

            if (name != other.name) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }
}

enum class PairAccessor {
    FST,
    SND
}
