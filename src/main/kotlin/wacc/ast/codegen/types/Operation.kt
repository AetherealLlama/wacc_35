package wacc.ast.codegen.types

sealed class Operation {
    object AddOp : Operation()
    object SubOp : Operation()
    object RevSubOp : Operation()
    object MulOp : Operation()
    data class DivOp(val signed: Boolean) : Operation()
    data class ModOp(val signed: Boolean) : Operation()
    object AndOp : Operation()
    object OrOp : Operation()
    object XorOp : Operation()
    object NegateOp : Operation()
}
