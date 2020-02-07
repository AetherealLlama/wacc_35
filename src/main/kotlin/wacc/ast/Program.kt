package wacc.ast

/**
 * The root node of our AST: the Program
 *
 * @property funcs the array of functions beginning a program
 * @property stat the program's statement
 */

data class Program(val funcs: Array<Func>, val stat: Stat) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Program

        if (!funcs.contentEquals(other.funcs)) return false

        return true
    }

    override fun hashCode(): Int {
        return funcs.contentHashCode()
    }
}