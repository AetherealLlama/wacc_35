package wacc.ast.codegen

import java.lang.IllegalStateException
import wacc.ast.*
import wacc.ast.BinaryOperator.*
import wacc.ast.UnaryOperator.*
import wacc.ast.codegen.types.*
import wacc.ast.codegen.types.Condition.*
import wacc.ast.codegen.types.ImmType.*
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Register.*

private const val MIN_USABLE_REG = 4
private const val MAX_USABLE_REG = 12

val usableRegs = (MIN_USABLE_REG..MAX_USABLE_REG).map { GeneralRegister(it) }

private class GlobalCodeGenData(var labelCount: Int = 0, var strings: List<String>) {
    fun getLabel() = "L${labelCount++}"

    fun getStringLabel(s: String): String =
            strings.indexOfFirst { s == it }.let { if (it < 0) strings.size.also { strings += s } else it }
                    .let { "msg_$it" }
}

private class CodeGenContext(
    val global: GlobalCodeGenData,
    val func: Func?,
    val vars: Map<String, Int>,
    val availableRegs: List<Register> = usableRegs
) {
    fun resolveIdent(ident: String): Int? =
            func?.params?.indexOfFirst { it.name == ident }?.let { if (it < 0) null else (it + 1) * 4 }
                    ?: vars[ident]

    fun withNewVar(name: String) = CodeGenContext(global, func, vars + (name to TODO()), availableRegs)

    fun takeReg(): Pair<Register, CodeGenContext>? =
            availableRegs.getOrNull(0)
                    ?.let { it to CodeGenContext(global, func, vars, availableRegs.drop(1)) }

    fun take2Regs(): Pair<Pair<Register, Register>, CodeGenContext>? =
            availableRegs.getOrNull(1)
                    ?.let { (availableRegs[0] to it) to CodeGenContext(global, func, vars, availableRegs.drop(2)) }

    fun withRegs(vararg regs: Register) = CodeGenContext(global, func, vars, regs.asList() + availableRegs)

    val dst: Register?
        get() = availableRegs.getOrNull(0)
}

fun Program.getAsm(): String {
    val (data, text) = genCode()
    val builder = StringBuilder()
    if (data.data.isNotEmpty()) {
        data.data.forEach { builder.append(it) }
    }
    builder.append(".text\n")
    text.functions.forEach { builder.append(it) }
    return builder.toString()
}

private fun Program.genCode(): Pair<Section.DataSection, Section.TextSection> {
    TODO()
//    val dataSection = Section.DataSection(emptyList())
//    val funcs = funcs.map(Func::codeGen).toMutableList()
//    funcs += stat.genMainFunc()
//    return Section.DataSection(emptyList()) to Section.TextSection(funcs)
}

// private fun Func.codeGen(): Function {
//    return Function(Label(name), emptyList(), false)
// }

private fun Stat.genCode(ctx: CodeGenContext): Pair<List<Instruction>, CodeGenContext> = when (this) {
    is Stat.Skip -> emptyList<Instruction>() to ctx
    is Stat.AssignNew -> rhs.genCode(ctx).let { rhsInstrs -> ctx.withNewVar(name).let { ctx2 ->
        (rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx2.resolveIdent(name)!!, INT))) to ctx2
    } }
    is Stat.Assign -> rhs.genCode(ctx).let { rhsInstrs -> when (lhs) {
        is AssignLhs.Variable ->
            (rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx.resolveIdent(lhs.name)!!, INT))) to ctx
        is AssignLhs.ArrayElem -> TODO()
        is AssignLhs.PairElem -> TODO()
    } }
    is Stat.Read -> TODO()
    is Stat.Free -> TODO()
    is Stat.Return -> (expr.genCode(ctx) + Move(GeneralRegister(0), Operand.Reg(ctx.dst!!))) to ctx
    is Stat.Exit -> TODO()
    is Stat.Print -> TODO()
    is Stat.Println -> TODO()
    is Stat.IfThenElse -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) -> (
            emptyList<Instruction>() +
                    expr.genCode(ctx) + // condition
                    Compare(ctx.dst!!, Imm(0, INT)) +
                    Branch(Operand.Label(label1), Equal) +
                    branch2.genCode(ctx).first + // code if false
                    Branch(Operand.Label(label2)) +
                    Special.Label(label1) +
                    branch1.genCode(ctx).first + // code if true
                    Special.Label(label2)
            ) to ctx }
    is Stat.WhileDo -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) -> (
            emptyList<Instruction>() +
                    Branch(Operand.Label(label1)) +
                    Special.Label(label2) +
                    stat.genCode(ctx).first + // loop body
                    Special.Label(label1) +
                    expr.genCode(ctx) + // loop condition
                    Compare(ctx.dst!!, Imm(1, INT)) +
                    Branch(Operand.Label(label1), Equal)
            ) to ctx }
    is Stat.Begin -> stat.genCode(ctx).first to ctx // ignore context from inner scope
    is Stat.Compose -> {
        val (instrsL, ctxL) = stat1.genCode(ctx)
        val (instrsR, ctxR) = stat2.genCode(ctxL)
        (instrsL + instrsR) to ctxR
    }
}

private fun AssignRhs.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is AssignRhs.Expression -> expr.genCode(ctx)
    is AssignRhs.ArrayLiteral -> TODO()
    is AssignRhs.Newpair -> TODO()
    is AssignRhs.PairElem -> TODO()
    is AssignRhs.Call -> TODO()
}

private fun Expr.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Expr.Literal.IntLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt(), INT))) // TODO: int vs long?
    is Expr.Literal.BoolLiteral -> listOf(Move(ctx.dst!!, Imm(if (value) 1 else 0, BOOL)))
    is Expr.Literal.CharLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt(), CHAR)))
    is Expr.Literal.StringLiteral -> listOf(Move(ctx.dst!!, Operand.Label(ctx.global.getStringLabel(value))))
    is Expr.Literal.PairLiteral -> throw IllegalStateException()
    is Expr.Ident -> listOf(Load(ctx.dst!!, Operand.Reg(StackPointer), Imm(ctx.resolveIdent(name)!!, INT)))
    is Expr.ArrayElem -> TODO()
    is Expr.UnaryOp -> when (operator) {
        BANG -> expr.genCode(ctx) + Op(Operation.NegateOp, ctx.dst!!, ctx.dst!!, Operand.Reg(ctx.dst!!))
        MINUS -> expr.genCode(ctx) + Op(Operation.RevSubOp, ctx.dst!!, ctx.dst!!, Imm(0, INT))
        LEN -> TODO()
        ORD, CHR -> expr.genCode(ctx) // Chars and ints should be represented the same way; ignore conversion
    }
    is Expr.BinaryOp -> ctx.take2Regs()?.let { (regs, ctx2) ->
        val (dst, nxt) = regs
        if (expr1.weight <= expr2.weight) {
            expr1.genCode(ctx2.withRegs(dst, nxt)) + expr2.genCode(ctx2.withRegs(nxt))
        } else {
            expr2.genCode(ctx2.withRegs(nxt, dst)) + expr2.genCode(ctx2.withRegs(dst))
        } + when (operator) {
            MUL -> listOf(Op(Operation.MulOp, dst, dst, Operand.Reg(nxt)))
            DIV -> listOf(Op(Operation.DivOp(TODO("Signed?")), dst, dst, Operand.Reg(nxt)))
            MOD -> listOf(Op(Operation.ModOp(TODO("Signed?")), dst, dst, Operand.Reg(nxt)))
            ADD -> listOf(Op(Operation.AddOp, dst, dst, Operand.Reg(nxt)))
            SUB -> listOf(Op(Operation.SubOp, dst, dst, Operand.Reg(nxt)))
            GT -> regs.assignBool(SignedGreaterThan)
            GTE -> regs.assignBool(SignedGreaterOrEqual)
            LT -> regs.assignBool(SignedLess)
            LTE -> regs.assignBool(SignedLessOrEqual)
            EQ -> regs.assignBool(Equal)
            NEQ -> regs.assignBool(NotEqual)
            LAND -> listOf(Op(Operation.AndOp, dst, dst, Operand.Reg(nxt)))
            LOR -> listOf(Op(Operation.OrOp, dst, dst, Operand.Reg(nxt)))
        }
    } ?: TODO()
}

// private fun Stat.genMainFunc(): Function {
//    // TODO remove hardcoded function
//    return Function(Label("main"), listOf(
//            Push(listOf(LinkRegister)),
//            Move(GeneralRegister(0), Imm(0, INT)),
//            Pop(listOf(ProgramCounter)),
//            Special.Ltorg
//    ), true)
// }

private fun Pair<Register, Register>.assignBool(cond: Condition) = listOf(
        Compare(first, Operand.Reg(second)),
        Move(first, Imm(1, BOOL), cond),
        Move(first, Imm(0, BOOL), cond.inverse)
)

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
