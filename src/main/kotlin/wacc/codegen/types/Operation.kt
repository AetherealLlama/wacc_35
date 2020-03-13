package wacc.codegen.types

sealed class Operation {
    object AddOp : Operation() {
        override fun toString(): String = "ADD"
    }

    object SubOp : Operation() {
        override fun toString(): String = "SUB"
    }

    object RevSubOp : Operation() {
        override fun toString(): String = "RSB"
    }

    object MulOp : Operation() {
        override fun toString(): String = "MUL"
    }

    data class DivOp(val signed: Boolean = true) : Operation()
    data class ModOp(val signed: Boolean = true) : Operation()

    object AndOp : Operation() {
        override fun toString(): String {
            return "AND"
        }
    }

    object OrOp : Operation() {
        override fun toString(): String = "ORR"
    }

    object XorOp : Operation() {
        override fun toString(): String = "EOR"
    }

    object NegateOp : Operation()

    object BitwiseAndOp : Operation() {
        override fun toString(): String = "AND"
    }
}
