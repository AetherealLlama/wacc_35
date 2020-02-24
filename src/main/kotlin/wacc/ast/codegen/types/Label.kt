package wacc.ast.codegen.types

data class Label(val name: String) {
    override fun toString(): String = "$name:\n"
}
