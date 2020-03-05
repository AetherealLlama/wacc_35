package wacc.codegen

import wacc.ast.AssignLhs
import wacc.ast.Stat
import wacc.ast.Type
import wacc.codegen.types.Condition.Equal
import wacc.codegen.types.Instruction
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Operand
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.R0
import wacc.codegen.types.Register.ProgramCounter
import wacc.codegen.types.Register.StackPointer

private fun Stat.AssignNew.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    rhs.genCode(ctx, instrs)
    instrs.add(Store(ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(name))))
}

private fun Stat.Assign.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    rhs.genCode(ctx, instrs)
    when (lhs) {
        is AssignLhs.Variable -> instrs.add(Store(ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(lhs.name))))
        is AssignLhs.ArrayElem -> TODO()
        is AssignLhs.PairElem -> TODO()
    }
}

private fun Stat.Read.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Op(AddOp, ctx.dst, StackPointer, Imm(0)))
    instrs.add(Move(R0, ctx.dst.op))
    when (type) {
        is Type.BaseType.TypeInt -> ctx.branchBuiltin(readInt, instrs)
        is Type.BaseType.TypeChar -> ctx.branchBuiltin(readChar, instrs)
        else -> throw IllegalStateException()
    }
    instrs.add(Load(ctx.dst, StackPointer.op))
}

private fun Stat.Free.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    ctx.branchBuiltin(freePair, instrs)
}

private fun Stat.Return.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    instrs.add(Pop(ProgramCounter))
}

private fun Stat.Exit.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    instrs.add(BranchLink(Operand.Label("exit")))
}

private fun Stat.Print.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))

    when (val type = this.type) {
        is Type.BaseType.TypeInt -> ctx.branchBuiltin(printInt, instrs)
        is Type.BaseType.TypeBool -> ctx.branchBuiltin(printBool, instrs)
        is Type.BaseType.TypeChar -> BranchLink(Operand.Label("putchar"))
        is Type.BaseType.TypeString -> ctx.branchBuiltin(printString, instrs)
        is Type.ArrayType -> when (type.type) {
            is Type.BaseType.TypeChar -> ctx.branchBuiltin(printString, instrs)
            else -> ctx.branchBuiltin(printReference, instrs)
        }
        is Type.PairType -> ctx.branchBuiltin(printReference, instrs)
        else -> throw IllegalStateException()
    }
}

private fun Stat.Println.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    Stat.Print(pos, expr).also { it.type = type }.genCode(ctx, instrs) // Reuse code gen for print
    ctx.branchBuiltin(printLn, instrs)
}

private fun Stat.IfThenElse.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val label1 = ctx.global.getLabel()
    val label2 = ctx.global.getLabel()

    expr.genCode(ctx, instrs) // condition
    instrs.add(Compare(ctx.dst, Imm(0)))
    instrs.add(Branch(Operand.Label(label1), Equal))
    branch2.genCodeWithNewScope(ctx, instrs) // code if false
    instrs.add(Branch(Operand.Label(label2)))
    instrs.add(Special.Label(label1))
    branch1.genCodeWithNewScope(ctx, instrs) // code if true
    instrs.add(Special.Label(label2))
}

private fun Stat.WhileDo.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val label1 = ctx.global.getLabel()
    val label2 = ctx.global.getLabel()

    instrs.add(Branch(Operand.Label(label1)))
    instrs.add(Special.Label(label2))
    stat.genCodeWithNewScope(ctx, instrs) // loop body
    instrs.add(Special.Label(label1))
    expr.genCode(ctx, instrs) // loop condition
    instrs.add(Compare(ctx.dst, Imm(1)))
    instrs.add(Branch(Operand.Label(label1), Equal))
}

private fun Stat.Begin.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    stat.genCodeWithNewScope(ctx, instrs)
}

private fun Stat.Compose.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    stat1.genCode(ctx, instrs)
    stat2.genCode(ctx, instrs)
}

// Delegates code gen to more specific functions
internal fun Stat.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is Stat.Skip -> Unit
    is Stat.AssignNew -> genCode(ctx, instrs)
    is Stat.Assign -> genCode(ctx, instrs)
    is Stat.Read -> genCode(ctx, instrs)
    is Stat.Free -> genCode(ctx, instrs)
    is Stat.Return -> genCode(ctx, instrs)
    is Stat.Exit -> genCode(ctx, instrs)
    is Stat.IfThenElse -> genCode(ctx, instrs)
    is Stat.WhileDo -> genCode(ctx, instrs)
    is Stat.Begin -> genCode(ctx, instrs)
    is Stat.Compose -> genCode(ctx, instrs)
    is Stat.Print -> genCode(ctx, instrs)
    is Stat.Println -> genCode(ctx, instrs)
}
