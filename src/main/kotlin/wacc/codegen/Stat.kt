package wacc.codegen

import wacc.ast.AssignLhs
import wacc.ast.PairAccessor
import wacc.ast.Stat
import wacc.ast.Type
import wacc.codegen.types.*
import wacc.codegen.types.Condition.Equal
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.Register.ProgramCounter
import wacc.codegen.types.Register.StackPointer

private fun Stat.AssignNew.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    rhs.genCode(ctx, instrs)
    val offset = Imm(ctx.offsetOfIdent(name, allowUndefined = true))
    val access = ctx.typeOfIdent(name, allowUndefined = true).memAccess
    instrs.add(Store(ctx.dst, StackPointer, offset, access = access))
    ctx.defineVar(name)
}

private fun Stat.Assign.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    rhs.genCode(ctx, instrs)
    when (lhs) {
        is AssignLhs.Variable -> {
            val (reg, offset, type) = lhs.cls?.let { cls ->
                lhs.classExpr!!.genCode(ctx.takeReg()!!.second, instrs)
                Triple(ctx.nxt, cls.offsetOfField(lhs.name), cls.typeOfField(lhs.name))
            } ?: Triple(StackPointer, ctx.offsetOfIdent(lhs.name), ctx.typeOfIdent(lhs.name))

            instrs.add(Store(
                    ctx.dst,
                    reg,
                    Imm(offset),
                    access = type.memAccess
            ))
        }
        is AssignLhs.ArrayElem -> {
            ctx.takeReg()!!.second.computeAddressOfArrayElem(lhs.name, lhs.exprs, instrs) // nxt = address of elem
            instrs.add(Store(ctx.dst, ctx.nxt, access = (ctx.typeOfIdent(lhs.name) as Type.ArrayType).type.memAccess))
        }
        is AssignLhs.PairElem -> {
            val offset = if (lhs.accessor == PairAccessor.FST) null else Imm(4)
            ctx.takeReg()!!.second.computeAddressOfPairElem(lhs.expr, instrs)
            instrs.add(Load(ctx.nxt, ctx.nxt.op, offset = offset))
            instrs.add(Store(ctx.dst, ctx.nxt, access = lhs.type.memAccess))
        }
    }
}

private fun Stat.Read.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    when (lhs) {
        is AssignLhs.Variable -> {
            val offset = lhs.cls?.let { cls ->
                lhs.classExpr!!.genCode(ctx, instrs)
                cls.offsetOfField(lhs.name)
            } ?: ctx.offsetOfIdent(lhs.name)
            instrs.add(Op(AddOp, ctx.dst, StackPointer, Imm(offset)))
        }
        is AssignLhs.ArrayElem -> {
            ctx.computeAddressOfArrayElem(lhs.name, lhs.exprs, instrs)
        }
        is AssignLhs.PairElem -> {
            val offset = if (lhs.accessor == PairAccessor.FST) Imm(0) else Imm(4)
            ctx.computeAddressOfPairElem(lhs.expr, instrs)
            instrs.add(Op(AddOp, ctx.dst, ctx.dst, offset))
        }
    }
    instrs.add(Move(R0, ctx.dst.op))
    when (type) {
        is Type.BaseType.TypeInt -> ctx.branchBuiltin(readInt, instrs)
        is Type.BaseType.TypeChar -> ctx.branchBuiltin(readChar, instrs)
        else -> throw IllegalStateException()
    }
}

private fun Stat.Free.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    val func = when (type) {
        is Type.PairType -> freePair
        is Type.ClassType -> freeInstance
        else -> throw IllegalStateException()
    }
    ctx.branchBuiltin(func, instrs)
}

private fun Stat.Return.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    instrs.add(Move(R0, ctx.dst.op))
    instrs.opWithConst(AddOp, ctx.totalScopeOffset, StackPointer)
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
        is Type.BaseType.TypeChar -> instrs.add(BranchLink(Operand.Label("putchar")))
        is Type.BaseType.TypeString -> ctx.branchBuiltin(printString, instrs)
        is Type.ArrayType -> when (type.type) {
            is Type.BaseType.TypeChar -> ctx.branchBuiltin(printString, instrs)
            else -> ctx.branchBuiltin(printReference, instrs)
        }
        is Type.PairType,
        is Type.ClassType -> ctx.branchBuiltin(printReference, instrs)
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
    branch1.genCodeWithNewScope(ctx, instrs) // code if false
    instrs.add(Branch(Operand.Label(label2)))
    instrs.add(Special.Label(label1))
    branch2.genCodeWithNewScope(ctx, instrs) // code if true
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
    instrs.add(Branch(Operand.Label(label2), Equal))
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
