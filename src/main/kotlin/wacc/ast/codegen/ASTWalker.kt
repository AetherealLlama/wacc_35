package wacc.ast.codegen

import wacc.ast.Expr
import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Stat
import java.lang.IllegalStateException

sealed class Arg {
    data class Register(val register: Int) : Arg()
    data class Immediate(val value: Long) : Arg()
    data class Address(val addr: Long) : Arg()
}

// TODO use this to hold variable mappings, etc.
// We should create a new one for each function traversal.
private class CodeGenContext

class ASTWalker {
    private var ctx = CodeGenContext()

    private fun Expr.Ident.toRegister(): Arg.Register = TODO()  // Use context to resolve identifier


    fun walk(program: Program): List<Instruction> =
            program.funcs.flatMap { walk(it).also { ctx = CodeGenContext() } } + walk(program.stat)

    fun walk(func: Func): List<Instruction> {
        // TODO: Handle parameters, pushing and popping regs, etc.
        return walk(func.stat)
    }

    fun walk(stat: Stat): List<Instruction> = when(stat) {
        is Stat.Skip -> emptyList()
        is Stat.AssignNew -> TODO()
        is Stat.Assign -> TODO()
        is Stat.Read -> TODO()
        is Stat.Free -> TODO()
        is Stat.Return -> TODO()
        is Stat.Exit -> TODO()
        is Stat.Print -> TODO()
        is Stat.Println -> TODO()
        is Stat.IfThenElse -> TODO()
        is Stat.WhileDo -> TODO()
        is Stat.Begin -> TODO()
        is Stat.Compose -> walk(stat.stat1) + walk(stat.stat2)
    }

    fun walk(expr: Expr): Pair<Arg?, List<Instruction>> = when(expr) {
        is Expr.Ident -> expr.toRegister() to emptyList()
        is Expr.Literal.IntLiteral -> Arg.Immediate(expr.value) to emptyList()
        is Expr.Literal.BoolLiteral -> Arg.Immediate(expr.value.asInt.toLong()) to emptyList()
        is Expr.Literal.CharLiteral -> Arg.Immediate(expr.value.asInt.toLong()) to emptyList()
        is Expr.Literal.StringLiteral -> TODO()
        is Expr.Literal.PairLiteral -> throw IllegalStateException()
        is Expr.ArrayElem -> TODO()
        is Expr.UnaryOp -> TODO()
        is Expr.BinaryOp -> TODO()
    }
}

val Boolean.asInt: Int
    get() = if (this) 1 else 0

val Char.asInt: Int
    get() = TODO()
