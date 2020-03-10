package wacc.ast

/**
 * A function AST Node
 *
 * @property pos the position in the source of the start of the function
 * @property type the return type of the function
 * @property name the name of the function, without parentheses
 * @property params the parameters passed to the function
 * @property stat the functions statement
 */

class Func(pos: FilePos, val type: Type, val name: String, val params: Array<Param>, val stat: Stat) : ASTNode(pos) {
    var overloadIx: Int = -1
    var cls: Class? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Func

        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun hashCode(): Int {
        return params.contentHashCode()
    }
}

/**
 * A parameter of a function above.
 *
 * @property pos the position in the source of the start of the parameter
 * @property type the type of the parameter
 * @property name the name of the parameter
 */

class Param(pos: FilePos, val type: Type, val name: String) : ASTNode(pos)
