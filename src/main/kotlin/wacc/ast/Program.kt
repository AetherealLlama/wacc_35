package wacc.ast

/**
 * The root node of our AST: the Program
 *
 * @property funcs the array of functions beginning a program
 * @property stat the program's statement
 */
data class Program(val classes: Array<Class>, val funcs: Array<Func>, val stat: Stat) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Program

        if (!classes.contentEquals(other.classes)) return false
        if (!funcs.contentEquals(other.funcs)) return false
        if (stat != other.stat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classes.contentHashCode()
        result = 31 * result + funcs.contentHashCode()
        result = 31 * result + stat.hashCode()
        return result
    }
}
