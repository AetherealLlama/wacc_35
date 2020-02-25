package wacc.ast.codegen.types

sealed class Register(private val display: String) {
    override fun toString(): String = display

    class GeneralRegister(number: Int) : Register("r$number")
    object StackPointer : Register("sp")
    object LinkRegister : Register("lr")
    object ProgramCounter : Register("pc")
}
