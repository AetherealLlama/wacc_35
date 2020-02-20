package wacc.ast.codegen.types

sealed class Instruction {
//    data class Load(val condition: Condition, val access: MemoryAccess, val reg: Register, )
    data class Move(val condition: Condition, val register: Register, val operand: Operand) : Instruction()
    data class Push(val condition: Condition, val regs: List<Register>) : Instruction()
    data class Pop(val condition: Condition, val regs: List<Register>) : Instruction()
    data class Branch(val condition: Condition, val operand: Operand) : Instruction()
    data class BranchLink(val condition: Condition, val operand: Operand) : Instruction()
}
