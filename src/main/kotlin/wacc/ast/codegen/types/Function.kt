package wacc.ast.codegen.types

data class Function(val label: String, val instructions: List<Instruction>, val main: Boolean = false) {
    override fun toString(): String {
        // TODO implement this
        return super.toString()
    }
}
