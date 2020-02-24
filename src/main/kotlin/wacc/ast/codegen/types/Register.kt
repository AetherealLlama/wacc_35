package wacc.ast.codegen.types

sealed class Register {
    data class GeneralRegister(val number: Int) : Register() {
        override fun toString(): String = "r$number"
    }

    object StackPointer : Register() {
        override fun toString(): String = "sp"
    }

    object LinkRegister : Register() {
        override fun toString(): String = "lr"
    }

    object ProgramCounter : Register() {
        override fun toString(): String = "pc"
    }
}
