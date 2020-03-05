package wacc.codegen.types

import wacc.ast.BinaryOperator.*
import wacc.ast.Expr
import wacc.ast.UnaryOperator.*
import wacc.codegen.*
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
    instrs.add(Load(ctx.dst, StackPointer.op, Imm(ctx.offsetOfIdent(name))))
}

private fun Expr.ArrayElem.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    instrs.add(Op(AddOp, ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(name.name))))
    for (expr in exprs) {
        ctx.takeReg()?.let{ (_, ctx2) ->
            expr.genCode(ctx2, instrs) // Register implementation
        } ?: let { // Stack implementation
            instrs.add(Push(listOf(ctx.dst))) // save array pointer
            expr.genCode(ctx, instrs)
            instrs.add(Move(ctx.nxt, ctx.dst.op)) // nxt = array index
            instrs.add(Pop(listOf(ctx.dst))) // dst = array pointer
        } // nxt = array index
        instrs.add(Load(ctx.dst, ctx.dst.op)) // get address of array
        instrs.add(Move(R0, ctx.nxt.op))
        instrs.add(Move(R1, ctx.dst.op))
        ctx.branchBuiltin(checkArrayBounds, instrs) // check array bounds
        instrs.add(Op(AddOp, ctx.dst, ctx.dst, Imm(4))) // compute address of desired array elem
        instrs.add(Op(AddOp, ctx.dst, ctx.dst, ctx.nxt.op, BarrelShift(2, BarrelShift.Type.LSL)))
    }
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
    ctx.takeRegs(2)?.let { (_, ctx2) -> // Register implementation
        if (expr1.weight <= expr2.weight) {
            expr1.genCode(ctx2.withRegs(ctx.dst, ctx.nxt), instrs)
            expr2.genCode(ctx2.withRegs(ctx.nxt), instrs)
        } else {
            expr2.genCode(ctx2.withRegs(ctx.nxt, ctx.dst), instrs)
            expr1.genCode(ctx2.withRegs(ctx.dst), instrs)
        }
    } ?: let { // Stack implementation
        expr1.genCode(ctx, instrs)
        expr2.genCode(ctx, instrs)
        instrs.add(Move(ctx.nxt, ctx.dst.op))
        instrs.add(Pop(listOf(ctx.dst)))
    }

    val regs = ctx.dst to ctx.nxt

    instrs + when (operator) {
        MUL -> {
            instrs.add(LongMul(ctx.dst, ctx.nxt, ctx.dst, ctx.nxt))
            instrs.add(Compare(ctx.nxt, ctx.dst.op, BarrelShift(31, BarrelShift.Type.ASR)))
            ctx.branchBuiltin(throwOverflowError, instrs, cond = Always)
        }
        DIV -> instrs.add(Op(DivOp(), ctx.dst, ctx.dst, ctx.nxt.op))
        MOD -> instrs.add(Op(ModOp(), ctx.dst, ctx.dst, ctx.nxt.op))
        ADD -> {
            instrs.add(Op(AddOp, ctx.dst, ctx.dst, ctx.nxt.op, setCondCodes = true))
            ctx.branchBuiltin(throwOverflowError, instrs, cond = Overflow)
        }
        SUB -> instrs.add(Op(SubOp, ctx.dst, ctx.dst, ctx.nxt.op))
        GT -> regs.assignBool(SignedGreaterThan, instrs)
        GTE -> regs.assignBool(SignedGreaterOrEqual, instrs)
        LT -> regs.assignBool(SignedLess, instrs)
        LTE -> regs.assignBool(SignedLessOrEqual, instrs)
        EQ -> regs.assignBool(Equal, instrs)
        NEQ -> regs.assignBool(NotEqual, instrs)
        LAND -> instrs.add(Op(AndOp, ctx.dst, ctx.dst, ctx.nxt.op))
        LOR -> instrs.add(Op(OrOp, ctx.dst, ctx.dst, ctx.nxt.op))
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
