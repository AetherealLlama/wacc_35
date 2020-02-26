package wacc.ast.codegen.types

import java.lang.IllegalStateException

sealed class Instruction {
    data class Op(
            val operation: Operation,
            val rd: Register,
            val rn: Register,
            val operand: Operand,
            val condition: Condition = Condition.Always
    ) : Instruction()

    data class Load(
            val rd: Register,
            val op: Operand,
            val offset: Operand? = null,
            val plus: Boolean = true,
            val access: MemoryAccess = MemoryAccess.Word,
            val condition: Condition = Condition.Always
    ) : Instruction() {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("LDR$condition $rd, ")
            builder.append(when (op) {
                is Operand.Imm -> "=${op.value}\n"
                is Operand.Label -> "=${op.label}\n"
                is Operand.Reg -> when (offset) {
                    is Operand.Reg -> "[${op.reg}, " + if (plus) "" else "-" + "${offset.reg}]\n"
                    is Operand.Imm ->
                        if (offset.value == 0)
                            "[${op.reg}]\n"
                        else
                            "[${op.reg}, #${offset.value}]\n"
                    null -> "[${op.reg}]\n"
                    is Operand.Label -> throw IllegalStateException()
                }
            })
            return builder.toString()
        }
    }

    data class Store(
            val rd: Register,
            val rn: Register,
            val offset: Operand? = null,
            val plus: Boolean = true,
            val access: MemoryAccess = MemoryAccess.Word,
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
        data class Label(val name: String) : Special() {
            override fun toString(): String = "$name:"
        }

        object Ltorg : Special() {
            override fun toString(): String = ".ltorg"
        }

        data class Global(val label: String) : Special()
    }
}
