package wacc.ast

import wacc.ast.codegen.types.MemoryAccess

/***
 * A statement AST Node
 *
 * Statement nodes representing different statements in the language specification,
 * extends the base ASTNode. Has different properties found in the specification.
 *
 * @property pos the position in the source of the start of the statement
 */
sealed class Stat(pos: FilePos, val vars: List<Pair<String, Type>> = emptyList()) : ASTNode(pos) {
    class Skip(pos: FilePos) : Stat(pos)
    class AssignNew(pos: FilePos, val type: Type, val name: String, val rhs: AssignRhs) : Stat(pos, listOf(name to type))
    class Assign(pos: FilePos, val lhs: AssignLhs, val rhs: AssignRhs) : Stat(pos)
    class Read(pos: FilePos, val lhs: AssignLhs) : Stat(pos)
    class Free(pos: FilePos, val expr: Expr) : Stat(pos)
    class Return(pos: FilePos, val expr: Expr) : Stat(pos)
    class Exit(pos: FilePos, val expr: Expr) : Stat(pos)
    class IfThenElse(pos: FilePos, val expr: Expr, val branch1: Stat, val branch2: Stat) : Stat(pos)
    class WhileDo(pos: FilePos, val expr: Expr, val stat: Stat) : Stat(pos)
    class Begin(pos: FilePos, val stat: Stat) : Stat(pos)
    class Compose(pos: FilePos, val stat1: Stat, val stat2: Stat) : Stat(pos, stat1.vars + stat2.vars)

    class Print(pos: FilePos, val expr: Expr) : Stat(pos) { lateinit var type: Type }
    class Println(pos: FilePos, val expr: Expr) : Stat(pos) { lateinit var type: Type }
}

/**
 * A statement fragment used in multiple statement cases representing either a variable
 * or array element.
 */
sealed class AssignLhs(pos: FilePos) : ASTNode(pos) {
    class Variable(pos: FilePos, val name: String) : AssignLhs(pos)
    class ArrayElem(pos: FilePos, val name: String, val exprs: Array<Expr>) : AssignLhs(pos) {
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

    class PairElem(pos: FilePos, val accessor: PairAccessor, val expr: Expr) : AssignLhs(pos)
}

/**
 * A statement fragment used in multiple statement cases: an expression, literal, pair element,
 * new pair or function return.
 */
sealed class AssignRhs(pos: FilePos) : ASTNode(pos) {
    class Expression(pos: FilePos, val expr: Expr) : AssignRhs(pos)
    class ArrayLiteral(pos: FilePos, val exprs: Array<Expr>) : AssignRhs(pos) {
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

    class Newpair(pos: FilePos, val expr1: Expr, val expr2: Expr) : AssignRhs(pos)
    class PairElem(pos: FilePos, val accessor: PairAccessor, val expr: Expr) : AssignRhs(pos)
    class Call(pos: FilePos, val name: String, val args: Array<Expr>) : AssignRhs(pos) {
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
