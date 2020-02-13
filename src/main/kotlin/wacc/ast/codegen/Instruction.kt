package wacc.ast.codegen

typealias Reg = Int
typealias Addr = Long

sealed class Instruction {
  data class Add(val rd: Reg, val rn: Reg, val op2: Reg, val carry: Boolean = false) : Instruction()
  data class And(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()
  data class BitClear(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()
  data class Branch(val address: Addr, val link: Boolean = false) : Instruction()
  data class Compare(val rn: Reg, val rd: Reg, val negative: Boolean = false) : Instruction()
  data class Load(val rd: Reg, val address: Addr) : Instruction()
  data class Move(val rd: Reg, val op2: Reg, val negative: Boolean = false) : Instruction()
  data class Multiply(val rd: Reg, val rm: Reg, val rs: Reg, val rn: Reg? = null) : Instruction()
  data class Or(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()
  data class Xor(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()

}