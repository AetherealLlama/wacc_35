package wacc.codegen.types

sealed class Operand {
    data class Imm(val value: Int, val type: ImmType = ImmType.INT) : Operand() {
        private val display: String = when (type) {
            ImmType.INT,
            ImmType.BOOL -> value.toString()
            ImmType.CHAR -> "'${value.toChar()}'"
        }

        override fun toString(): String = "#$display"
    }

    data class Reg(val reg: Register) : Operand()
    data class Label(val label: String) : Operand()
}

enum class ImmType {
    INT,
    BOOL,
    CHAR
}
