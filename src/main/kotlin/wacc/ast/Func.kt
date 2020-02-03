package wacc.ast

data class Func(val type: Type, val name: String, val params: Array<Param>, val stat: Stat) {
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

data class Param(val type: Type, val name: String)
