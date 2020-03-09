package wacc.codegen

import wacc.ast.BinaryOperator.*
import wacc.ast.Expr
import wacc.ast.UnaryOperator.*
import wacc.codegen.types.*
import wacc.codegen.types.Condition.*
import wacc.codegen.types.ImmType.BOOL
import wacc.codegen.types.ImmType.CHAR
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operation.*
import wacc.codegen.types.Register.StackPointer

private fun Expr.Literal.IntLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    if (value in 0..255)
        instrs.add(Move(ctx.dst, Imm(value.toInt())))
    else
        instrs.add(Load(ctx.dst, Imm(value.toInt())))
}

private fun Expr.Literal.BoolLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Move(ctx.dst, Imm(if (value) 1 else 0, BOOL)))
}

private fun Expr.Literal.CharLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Move(ctx.dst, Imm(value.toInt(), CHAR)))
}

private fun Expr.Literal.StringLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Load(ctx.dst, Operand.Label(ctx.global.getStringLabel(value))))
}

private fun Expr.Ident.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Load(ctx.dst, StackPointer.op, Imm(ctx.offsetOfIdent(name)), access = ctx.typeOfIdent(name).memAccess))
}

private fun Expr.ArrayElem.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    ctx.computeAddressOfArrayElem(name.name, exprs, instrs)
    instrs.add(Load(ctx.dst, ctx.dst.op))
}

private fun Expr.UnaryOp.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
    when (operator) {
        BANG -> instrs.add(Op(NegateOp, ctx.dst, ctx.dst, ctx.dst.op))
        MINUS -> Op(RevSubOp, ctx.dst, ctx.dst, Imm(0))
        LEN -> Load(ctx.dst, ctx.dst.op)
        ORD, CHR -> {} // Chars and ints should be represented the same way; ignore conversion
    }
}

private fun Expr.BinaryOp.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val regs = ctx.takeRegs(2, force = true)?.let { (_, ctx2) -> // Register implementation
        if (expr1.weight <= expr2.weight) {
            expr1.genCode(ctx2.withRegs(ctx.dst, ctx.nxt), instrs)
            expr2.genCode(ctx2.withRegs(ctx.nxt), instrs)
        } else {
            expr2.genCode(ctx2.withRegs(ctx.nxt, ctx.dst), instrs)
            expr1.genCode(ctx2.withRegs(ctx.dst), instrs)
        }
        ctx.dst to ctx.nxt
    } ?: let { // Stack implementation
        expr1.genCode(ctx, instrs)
        instrs.add(Push(ctx.dst))
        expr2.genCode(ctx, instrs)
        instrs.add(Pop(ctx.nxt))
        ctx.nxt to ctx.dst
    }

    instrs + when (operator) {
        MUL -> {
            instrs.add(LongMul(ctx.dst, ctx.nxt, regs.first, regs.second))
            instrs.add(Compare(ctx.nxt, ctx.dst.op, BarrelShift(31, BarrelShift.Type.ASR)))
            ctx.branchBuiltin(throwOverflowError, instrs, cond = NotEqual)
        }
        DIV -> {
            instrs.add(Move(R0, regs.first.op))
            instrs.add(Move(R1, regs.second.op))
            ctx.branchBuiltin(checkDivideByZero, instrs)
            instrs.add(BranchLink(Operand.Label("__aeabi_idiv")))
            instrs.add(Move(ctx.dst, R0.op))
        }
        MOD -> {
            instrs.add(Move(R0, regs.first.op))
            instrs.add(Move(R1, regs.second.op))
            ctx.branchBuiltin(checkDivideByZero, instrs)
            instrs.add(BranchLink(Operand.Label("__aeabi_idivmod")))
            instrs.add(Move(ctx.dst, R1.op))
        }
        ADD -> {
            instrs.add(Op(AddOp, ctx.dst, regs.first, regs.second.op, setCondCodes = true))
            ctx.branchBuiltin(throwOverflowError, instrs, cond = Overflow)
        }
        SUB -> instrs.add(Op(SubOp, ctx.dst, regs.first, regs.second.op))
        GT -> regs.assignBool(SignedGreaterThan, instrs)
        GTE -> regs.assignBool(SignedGreaterOrEqual, instrs)
        LT -> regs.assignBool(SignedLess, instrs)
        LTE -> regs.assignBool(SignedLessOrEqual, instrs)
        EQ -> regs.assignBool(Equal, instrs)
        NEQ -> regs.assignBool(NotEqual, instrs)
        LAND -> instrs.add(Op(AndOp, ctx.dst, regs.first, regs.second.op))
        LOR -> instrs.add(Op(OrOp, ctx.dst, regs.first, regs.second.op))
    }
}

// Delegates code gen to more specific functions
internal fun Expr.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is Expr.Literal.IntLiteral -> genCode(ctx, instrs)
    is Expr.Literal.BoolLiteral -> genCode(ctx, instrs)
    is Expr.Literal.CharLiteral -> genCode(ctx, instrs)
    is Expr.Literal.StringLiteral -> genCode(ctx, instrs)
    is Expr.Literal.PairLiteral -> throw IllegalStateException()
    is Expr.Ident -> genCode(ctx, instrs)
    is Expr.ArrayElem -> genCode(ctx, instrs)
    is Expr.UnaryOp -> genCode(ctx, instrs)
    is Expr.BinaryOp -> genCode(ctx, instrs)
}

private fun Pair<Register, Register>.assignBool(cond: Condition, instrs: MutableList<Instruction>) {
    instrs.add(Compare(first, second.op))
    instrs.add(Move(first, Imm(1, BOOL), cond))
    instrs.add(Move(first, Imm(0, BOOL), cond.inverse))
}

private val Condition.inverse
    get() = when (this) {
        Minus -> Plus
        Plus -> Minus
        Equal -> NotEqual
        NotEqual -> Equal
        UnsignedHigherOrSame -> UnsignedLower
        UnsignedLower -> UnsignedHigherOrSame
        CarrySet -> CarryClear
        CarryClear -> CarrySet
        Overflow -> NoOverflow
        NoOverflow -> Overflow
        UnsignedHigher -> UnsignedLowerOrSame
        UnsignedLowerOrSame -> UnsignedHigher
        SignedGreaterOrEqual -> SignedLess
        SignedLess -> SignedGreaterOrEqual
        SignedGreaterThan -> SignedLessOrEqual
        SignedLessOrEqual -> SignedGreaterThan
        Always -> throw IllegalStateException()
    }
