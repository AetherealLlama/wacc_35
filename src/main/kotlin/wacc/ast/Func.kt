package wacc.ast

class Func(pos: FilePos, val type: Type, val name: String, val params: Array<Param>, val stat: Stat) : ASTNode(pos) {
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

class Param(pos: FilePos, val type: Type, val name: String) : ASTNode(pos)
