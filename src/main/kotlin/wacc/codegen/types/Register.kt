package wacc.codegen.types

sealed class Register(private val display: String) {
    override fun toString(): String = display

    class GeneralRegister(number: Int) : Register("r$number")
    object StackPointer : Register("sp")
    object LinkRegister : Register("lr")
    object ProgramCounter : Register("pc")
}

val R0 = Register.GeneralRegister(0)
val R1 = Register.GeneralRegister(1)
val R2 = Register.GeneralRegister(2)
val R3 = Register.GeneralRegister(3)
