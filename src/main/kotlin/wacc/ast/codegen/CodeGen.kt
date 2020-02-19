package wacc.ast.codegen

import wacc.ast.Expr
import wacc.ast.Program
import wacc.ast.Func
import wacc.ast.Stat
import wacc.ast.codegen.types.Instruction

fun Program.genCode(): List<Instruction> {
  return funcs.flatMap { it.genCode() } + stat.genCode()
}

fun Stat.genCode(): List<Instruction> {
  when (this) {
    is Stat.Skip -> TODO()
    is Stat.AssignNew -> TODO()
    is Stat.Assign-> TODO()
    is Stat.Read -> TODO()
    is Stat.Free -> TODO()
    is Stat.Return -> TODO()
    is Stat.Exit -> TODO()
    is Stat.Print -> TODO()
    is Stat.Println -> TODO()
    is Stat.IfThenElse -> TODO()
    is Stat.WhileDo -> TODO()
    is Stat.Begin -> TODO()
    is Stat.Compose -> TODO()
  }
}

fun Func.genCode(): List<Instruction> {
  return TODO()
}

fun Expr.genCode() {
  return TODO()
}