package wacc.ast.codegen.types

sealed class Instruction {
    data class Op(val operation: Operation, val condition: Condition, val rd: Register, val rn: Register, val operand: Operand) : Instruction()
    // TODO handle LDR pseudo-instruction for immediate values
    data class Load(val condition: Condition, val access: MemoryAccess, val rd: Register, val rn: Register, val offset: Operand, val plus: Boolean) : Instruction()

    data class Store(val condition: Condition, val access: MemoryAccess, val rd: Register, val rn: Register, val offset: Operand, val plus: Boolean) : Instruction()

    data class Move(val condition: Condition, val reg: Register, val operand: Operand) : Instruction() {
        override fun toString(): String = "MOV$condition $reg, $operand\n"
    }

    data class Compare(val condition: Condition, val reg: Register, val operand: Operand) : Instruction()

    data class Push(val condition: Condition = Condition.Always, val regs: List<Register>) : Instruction() {
        override fun toString(): String = "PUSH$condition {${regs.joinToString()}}\n"
    }

    data class Pop(val condition: Condition, val regs: List<Register>) : Instruction() {
        override fun toString(): String = "POP$condition {${regs.joinToString()}}\n"
    }

    data class Branch(val condition: Condition, val operand: Operand) : Instruction()
    data class BranchLink(val condition: Condition, val operand: Operand) : Instruction()
    data class Shift(val condition: Condition, val shift: LShift, val rd: Register, val rm: Register, val operand: Operand) : Instruction()

    sealed class Special : Instruction() {
        object Ltorg : Special() {
            override fun toString(): String = ".ltorg"
        }

        data class Global(val label: String) : Special()
    }
}
