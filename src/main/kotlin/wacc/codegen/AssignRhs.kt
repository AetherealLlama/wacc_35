package wacc.codegen

import wacc.ast.AssignRhs
import wacc.ast.PairAccessor
import wacc.ast.Param
import wacc.ast.Type
import wacc.codegen.types.Instruction
import wacc.codegen.types.Instruction.*
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
        instrs.add(Store(innerCtx.dst, arrayAddr, Imm(4 + i * type.size), access = type.memAccess))
    }
    instrs.add(Load(innerCtx.dst, Imm(exprs.size)))
    instrs.add(Store(innerCtx.dst, arrayAddr))
}

private fun AssignRhs.Newpair.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    ctx.malloc(8, instrs)
    val (pairReg, ctx2) = ctx.takeReg()!!
    for ((expr, type, offset) in listOf(Triple(expr1, types.first, null), Triple(expr2, types.second, Imm(4)))) {
        expr.genCode(ctx2, instrs)
        instrs.add(Load(R0, Imm(type.size)))
        instrs.add(BranchLink(Operand.Label("malloc")))
        instrs.add(Store(ctx2.dst, R0, access = type.memAccess))
        instrs.add(Store(R0, pairReg, offset))
    }
}

private fun AssignRhs.PairElem.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val offset = if (accessor == PairAccessor.FST) null else Imm(4)
    ctx.computeAddressOfPairElem(expr, instrs)
    instrs.add(Load(ctx.dst, ctx.dst.op, offset))
    instrs.add(Load(ctx.dst, ctx.dst.op, Imm(0), access = type.memAccess))
}

private fun AssignRhs.Call.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val func = (cls?.funcs ?: ctx.global.program.funcs.asList())
            .first { it.name == name && it.overloadIx == overloadIx }
    var params = func.params.map(Param::type).zip(args).reversed()
    cls?.let { params = params + (Type.ClassType(it.name) to classExpr!!) }
    var totalOffset = 0
    if (params.isNotEmpty()) {
        for ((type, expr) in params) {
            expr.genCode(ctx.withStackOffset(totalOffset), instrs)
            instrs.add(Store(ctx.dst, StackPointer, Imm(type.size), plus = false, moveReg = true, access = type.memAccess))
            totalOffset += type.size
        }
        instrs.add(BranchLink(Operand.Label(func.label)))
        instrs.opWithConst(AddOp, totalOffset, StackPointer)
    } else {
        instrs.add(BranchLink(Operand.Label(func.label)))
    }
    instrs.add(Move(ctx.dst, R0.op))
}

// Delegates code gen to more specific functions
internal fun AssignRhs.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is AssignRhs.Expression -> genCode(ctx, instrs)
    is AssignRhs.ArrayLiteral -> genCode(ctx, instrs)
    is AssignRhs.Newpair -> genCode(ctx, instrs)
    is AssignRhs.PairElem -> genCode(ctx, instrs)
    is AssignRhs.Call -> genCode(ctx, instrs)
}
