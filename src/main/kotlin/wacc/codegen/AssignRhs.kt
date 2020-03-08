package wacc.codegen

import wacc.ast.AssignRhs
import wacc.ast.PairAccessor
import wacc.ast.Param
import wacc.ast.Type.BaseType.TypeChar
import wacc.codegen.types.Instruction
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.MemoryAccess
import wacc.codegen.types.Operand
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.R0
import wacc.codegen.types.Register.StackPointer

private fun AssignRhs.Expression.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
}

private fun AssignRhs.ArrayLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val (arrayAddr, innerCtx) = ctx.takeReg()!!

    ctx.malloc((exprs.size * type.size) + 4, instrs)
    for ((i, expr) in exprs.withIndex()) {
        expr.genCode(innerCtx, instrs)
        val access = if (type == TypeChar) MemoryAccess.Byte else MemoryAccess.Word
        instrs.add(Store(innerCtx.dst, arrayAddr, Imm(4 + i * type.size), access = access))
    }
    instrs.add(Load(innerCtx.dst, Imm(exprs.size)))
    instrs.add(Store(innerCtx.dst, arrayAddr))
}

private fun AssignRhs.Newpair.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    ctx.malloc(8, instrs)
    val (pairReg, ctx2) = ctx.takeReg()!!
    for ((expr, offset) in listOf(expr1 to null, expr2 to Imm(4))) {
        expr.genCode(ctx2, instrs)
        instrs.add(Load(R0, Imm(4)))
        instrs.add(BranchLink(Operand.Label("malloc")))
        instrs.add(Store(ctx2.dst, R0))
        instrs.add(Store(R0, pairReg, offset))
    }
}

private fun AssignRhs.PairElem.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val offset = if (accessor == PairAccessor.FST) null else Imm(4)

    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    ctx.branchBuiltin(checkNullPointer, instrs)
    instrs.add(Load(ctx.dst, ctx.dst.op, offset))
}

private fun AssignRhs.Call.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val func = ctx.global.program.funcs.first { it.name == name }
    var totalOffset = 0
    if (func.params.isNotEmpty()) {
        for ((type, expr) in func.params.map(Param::type).zip(args).reversed()) {
            expr.genCode(ctx.withStackOffset(totalOffset), instrs)
            instrs.add(Store(ctx.dst, StackPointer, Imm(type.size), plus = false, moveReg = true))
            totalOffset += type.size
        }
        instrs.add(BranchLink(Operand.Label(func.label)))
        instrs.opWithConst(AddOp, totalOffset, StackPointer)
    } else {
        instrs.add(BranchLink(Operand.Label(func.label)))
    }
}

// Delegates code gen to more specific functions
internal fun AssignRhs.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is AssignRhs.Expression -> genCode(ctx, instrs)
    is AssignRhs.ArrayLiteral -> genCode(ctx, instrs)
    is AssignRhs.Newpair -> genCode(ctx, instrs)
    is AssignRhs.PairElem -> genCode(ctx, instrs)
    is AssignRhs.Call -> genCode(ctx, instrs)
}

private fun CodeGenContext.malloc(size: Int, instrs: MutableList<Instruction>) {
    instrs.add(Load(R0, Imm(size)))
    instrs.add(BranchLink(Operand.Label("malloc")))
    if (dst.toString() != R0.toString())
        instrs.add(Move(dst, R0.op))
}
