package wacc.codegen.types

import java.lang.IllegalStateException

typealias Function = List<Instruction>

sealed class Instruction {
    abstract val asAsm: String

    data class Op(
        val operation: Operation,
        val rd: Register,
        val rn: Register,
        val operand: Operand,
        val shift: BarrelShift? = null,
        val condition: Condition = Condition.Always,
        val setCondCodes: Boolean = false
    ) : Instruction() {
        override val asAsm: String =
                "\t" + when (operation) {
                    is Operation.NegateOp -> "EOR$condition $rd, $rn, #1"
                    else -> "$operation${if (setCondCodes) "S" else ""}$condition $rd, $rn, " + when (operand) {
                        is Operand.Imm -> "#${operand.value}"
                        is Operand.Reg -> operand.reg
                        is Operand.Label -> throw IllegalStateException()
                    }
                } + (shift?.let { ", ${it.asAsm}" } ?: "")
    }

    data class Load(
        val rd: Register,
        val op: Operand,
        val offset: Operand? = null,
        val plus: Boolean = true,
        val access: MemoryAccess = MemoryAccess.Word,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String

        init {
            val access = if (this.access == MemoryAccess.Byte) MemoryAccess.SignedByte else this.access
            val builder = StringBuilder("\t")
            builder.append("LDR$condition$access $rd, ")
            builder.append(when (op) {
                is Operand.Imm -> "=${op.value}"
                is Operand.Label -> "=${op.label}"
                is Operand.Reg -> when (offset) {
                    is Operand.Reg -> "[${op.reg}, " + if (plus) "" else "-" + "${offset.reg}]"
                    is Operand.Imm ->
                        if (offset.value == 0)
                            "[${op.reg}]"
                        else
                            "[${op.reg}, #${offset.value}]"
                    null -> "[${op.reg}]"
                    is Operand.Label -> throw IllegalStateException()
                }
            })
            asAsm = builder.toString()
        }
    }

    data class LongMul(
        val rdLo: Register,
        val rdHi: Register,
        val rm: Register,
        val rs: Register
    ) : Instruction() {
        override val asAsm: String = "\tSMULL $rdLo, $rdHi, $rm, $rs"
    }

    data class Store(
        val rd: Register,
        val rn: Register,
        val offset: Operand? = null,
        val plus: Boolean = true,
        val moveReg: Boolean = false,
        val access: MemoryAccess = MemoryAccess.Word,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String =
                "\tSTR$condition$access $rd, [$rn" + when (offset) {
                    is Operand.Imm -> if (offset.value == 0) "" else ", #${if (plus) offset.value else -offset.value}"
                    is Operand.Reg -> ", " + if (plus) "" else "-" + "${offset.reg}"
                    is Operand.Label -> throw IllegalStateException()
                    null -> ""
                } + "]" + (if (moveReg) "!" else "")
    }

    data class Move(
        val reg: Register,
        val operand: Operand,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String =
                "\tMOV$condition $reg, " + when (operand) {
                    is Operand.Imm -> operand.toString()
                    is Operand.Reg -> "${operand.reg}"
                    is Operand.Label -> throw IllegalStateException()
                }
    }

    data class Compare(
        val reg: Register,
        val operand: Operand,
        val shift: BarrelShift? = null,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String = "\tCMP$condition $reg, " + when (operand) {
            is Operand.Imm -> "#${operand.value}"
            is Operand.Reg -> "${operand.reg}"
            is Operand.Label -> throw IllegalStateException()
        } + (shift?.let { ", ${it.asAsm}" } ?: "")
    }

    data class Push(
        val regs: List<Register>,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        constructor(vararg regs: Register, condition: Condition = Condition.Always) : this(regs.asList(), condition)

        override val asAsm: String = "\tPUSH$condition {${regs.joinToString()}}"
    }

    data class Pop(
        val regs: List<Register>,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        constructor(vararg regs: Register, condition: Condition = Condition.Always) : this(regs.asList(), condition)

        override val asAsm: String = "\tPOP$condition {${regs.joinToString()}}"
    }

    data class Branch(
        val operand: Operand,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String = "\t" + when (operand) {
            is Operand.Label -> "B$condition ${operand.label}"
            else -> throw IllegalStateException()
        }
    }

    data class BranchLink(
        val operand: Operand,
        val condition: Condition = Condition.Always
    ) : Instruction() {
        override val asAsm: String = "\t" + when (operand) {
            is Operand.Label -> "BL$condition ${operand.label}"
            else -> throw IllegalStateException()
        }
    }

    sealed class Special : Instruction() {
        data class Label(val name: String) : Special() {
            override val asAsm: String = "$name:"
        }

        object Ltorg : Special() {
            override val asAsm: String = "\t.ltorg"
        }

        data class Global(val label: String) : Special() {
            override val asAsm: String = ".global $label"
        }
    }
}

data class BarrelShift(val amount: Int, val type: Type) {
    enum class Type {
        LSL,
        ASR
    }

    val asAsm: String = "$type #$amount"
}
