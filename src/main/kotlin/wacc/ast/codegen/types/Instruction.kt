package wacc.ast.codegen.types

sealed class Instruction {
    data class Load(val condition: Condition, val access: MemoryAccess, val rd: Register, val rn: Register, val offset: Operand, val plus: Boolean) : Instruction()
    data class Store(val condition: Condition, val access: MemoryAccess, val rd: Register, val rn: Register, val offset: Operand, val plus: Boolean) : Instruction()
    data class Move(val condition: Condition, val reg: Register, val operand: Operand) : Instruction()
    data class Compare(val condition: Condition, val reg: Register, val operand: Operand) : Instruction()
    data class Push(val condition: Condition, val regs: List<Register>) : Instruction()
    data class Pop(val condition: Condition, val regs: List<Register>) : Instruction()
    data class Branch(val condition: Condition, val operand: Operand) : Instruction()
    data class BranchLink(val condition: Condition, val operand: Operand) : Instruction()
    data class Shift(val condition: Condition, val shift: wacc.ast.codegen.types.Shift, val rd: Register, val rm: Register, val operand: Operand) : Instruction()
}
