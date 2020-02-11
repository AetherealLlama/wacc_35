package wacc.ast.codegen

typealias Reg = Int
typealias Addr = Long

sealed class Instruction {

  class Add(val rd: Reg, val rn: Reg, val op2: Reg, val carry: Boolean = false) : Instruction()
  class And(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()
  class BitClear(val rd: Reg, val rn: Reg, val op2: Reg)
  class Branch(val address: Addr, val link: Boolean = false)
  class Compare(val rn: Reg, val rd: Reg, val negative: Boolean = false)
  class Load(val rd: Reg, val address: Addr)
  class Move(val rd: Reg, val op2: Reg, val negative: Boolean = false)
  class Multiply(val rd: Reg, val rm: Reg, val rs: Reg, val rn: Reg? = null)
  class Or(val rd: Reg, val rn: Reg, val op2: Reg)
  class Xor(val rd: Reg, val rn: Reg, val op2: Reg) : Instruction()

}