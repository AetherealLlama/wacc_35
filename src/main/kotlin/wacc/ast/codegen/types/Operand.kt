package wacc.ast.codegen.types

sealed class Operand {
    data class Imm(val value: Int) : Operand() {
        override fun toString(): String = "#$value"
    }

    data class Reg(val reg: Register) : Operand()
    data class Label(val label: String) : Operand()
}
