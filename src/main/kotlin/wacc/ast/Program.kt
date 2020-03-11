package wacc.ast

/**
 * The root node of our AST: the Program
 *
 * @property funcs the array of functions beginning a program
 * @property stat the program's statement
 */
data class Program(val includes: Array<Include>?, val classes: Array<Class>, val funcs: Array<Func>, val stat: Stat?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Program

        if (includes != null) {
            if (other.includes == null) return false
            if (!includes.contentEquals(other.includes)) return false
        } else if (other.includes != null) return false
        if (!classes.contentEquals(other.classes)) return false
        if (!funcs.contentEquals(other.funcs)) return false
        if (stat != other.stat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = includes?.contentHashCode() ?: 0
        result = 31 * result + classes.contentHashCode()
        result = 31 * result + funcs.contentHashCode()
        result = 31 * result + (stat?.hashCode() ?: 0)
        return result
    }
}
