package wacc.ast.codegen.types

sealed class Instruction {
    data class Op(
            val operation: Operation,
            val rd: Register,
            val rn: Register,
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    // TODO handle LDR pseudo-instruction for immediate values
    data class Load(
            val access: MemoryAccess,
            val rd: Register,
            val rn: Register,
            val offset: Operand,
            val plus: Boolean,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class Store(
            val access: MemoryAccess,
            val rd: Register,
            val rn: Register,
            val offset: Operand,
            val plus: Boolean,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class Move(
            val reg: Register,
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction() {
        override fun toString(): String = "MOV$condition $reg, $operand\n"
    }

    data class Compare(
            val reg: Register,
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class Push(
            val regs: List<Register>,
            val condition: Condition = Condition.Always
    ) : Instruction() {
        override fun toString(): String = "PUSH$condition {${regs.joinToString()}}\n"
    }

    data class Pop(
            val regs: List<Register>,
            val condition: Condition = Condition.Always
    ) : Instruction() {
        override fun toString(): String = "POP$condition {${regs.joinToString()}}\n"
    }


    data class Branch(
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class BranchLink(
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class Shift(
            val shift: LShift,
            val rd: Register,
            val rm: Register,
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    sealed class Special : Instruction() {
        object Ltorg : Special() {
            override fun toString(): String = ".ltorg"
        }

        data class Global(val label: String) : Special()
    }
}
