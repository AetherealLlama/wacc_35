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

/*
We don't need to worry about register vs. stack allocation when dealing with Stat and AssignRhs
since there shouldn't be anytime when registers are "reserved" from previous statements.
 */

private const val MIN_USABLE_REG = 4
private const val MAX_USABLE_REG = 12

val R0 = GeneralRegister(0)

val usableRegs = (MIN_USABLE_REG..MAX_USABLE_REG).map { GeneralRegister(it) }

private class GlobalCodeGenData(
    var labelCount: Int = 0,
    var strings: List<String>,
    val program: Program
) {
    fun getLabel() = "L${labelCount++}"

    val usedBuiltins: MutableSet<BuiltinFunction> = mutableSetOf()

    fun getStringLabel(s: String): String =
            strings.indexOfFirst { s == it }.let { if (it < 0) strings.size.also { strings += s } else it }
                    .let { "msg_$it" }
}

private class CodeGenContext(
    val global: GlobalCodeGenData,
    val func: Func?,
    val stackOffset: Int,
    val scopes: List<List<Pair<String, Type>>>,
    val availableRegs: List<Register> = usableRegs
) {
    fun offsetOfIdent(ident: String): Int {
        var offset = stackOffset
        var found = false
        for (scope in scopes) {
            for (varData in scope) {
                val (name, memAcc) = varData
                if (found)
                    offset += memAcc.size
                found = found || (name == ident)
            }
            if (found)
                break
        }
        if (!found)
            throw IllegalStateException()
        return offset
    }

    fun takeReg(): Pair<Register, CodeGenContext>? =
            availableRegs.getOrNull(0)?.let { reg ->
                reg to CodeGenContext(global, func, stackOffset, scopes, availableRegs.drop(1))
            }

    fun withNewScope(newScope: List<Pair<String, Type>>): CodeGenContext =
            CodeGenContext(global, func, stackOffset, listOf(newScope) + scopes, availableRegs)

    fun takeRegs(n: Int): Pair<List<Register>, CodeGenContext>? =
            if (availableRegs.size < n)
                null
            else
                availableRegs.take(n) to CodeGenContext(global, func, stackOffset, scopes, availableRegs.drop(n))

    fun withRegs(vararg regs: Register) =
            CodeGenContext(global, func, stackOffset, scopes, regs.asList() + availableRegs)

    fun withStackOffset(offset: Int) =
            CodeGenContext(global, func, offset, scopes, availableRegs)

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

private fun Stat.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Stat.Skip -> emptyList()
    is Stat.AssignNew -> rhs.genCode(ctx).let { rhsInstrs ->
        rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx.offsetOfIdent(name)))
    }
    is Stat.Assign -> rhs.genCode(ctx).let { rhsInstrs ->
        when (lhs) {
            is AssignLhs.Variable -> rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx.offsetOfIdent(lhs.name)))
            is AssignLhs.ArrayElem -> TODO()
            is AssignLhs.PairElem -> TODO()
        }
    }
    is Stat.Read -> TODO()
    is Stat.Free -> expr.genCode(ctx) + Move(R0, ctx.dst!!.op) + ctx.branchBuiltin(freePair)
    is Stat.Return -> expr.genCode(ctx) + Move(R0, ctx.dst!!.op) + Pop(listOf(ProgramCounter))
    is Stat.Exit ->
        expr.genCode(ctx) + Move(R0, ctx.dst!!.op) + BranchLink(Operand.Label("exit"))
    is Stat.Print -> expr.genCode(ctx) + Move(R0, ctx.dst!!.op) + when (type) {
        is Type.BaseType.TypeInt -> ctx.branchBuiltin(printInt)
        is Type.BaseType.TypeBool -> ctx.branchBuiltin(printBool)
        is Type.BaseType.TypeChar -> BranchLink(Operand.Label("putchar"))
        is Type.BaseType.TypeString -> ctx.branchBuiltin(printString)
        is Type.ArrayType -> when (type) {
            is Type.BaseType.TypeChar -> ctx.branchBuiltin(printString)
            else -> ctx.branchBuiltin(printReference)
        }
        is Type.PairType -> ctx.branchBuiltin(printReference)
        else -> throw IllegalStateException()
    }
    is Stat.Println -> Stat.Print(pos, expr).genCode(ctx) + BranchLink(Operand.Label(printLn.function.label.name)).also { ctx.global.usedBuiltins.add(printLn) }
    is Stat.IfThenElse -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) ->
        emptyList<Instruction>() +
                expr.genCode(ctx) + // condition
                Compare(ctx.dst!!, Imm(0)) +
                Branch(Operand.Label(label1), Equal) +
                branch2.genCode(ctx) + // code if false
                Branch(Operand.Label(label2)) +
                Special.Label(label1) +
                branch1.genCode(ctx) + // code if true
                Special.Label(label2)
    }
    is Stat.WhileDo -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) ->
        emptyList<Instruction>() +
                Branch(Operand.Label(label1)) +
                Special.Label(label2) +
                stat.genCodeWithNewScope(ctx) + // loop body
                Special.Label(label1) +
                expr.genCode(ctx) + // loop condition
                Compare(ctx.dst!!, Imm(1)) +
                Branch(Operand.Label(label1), Equal)
    }
    is Stat.Begin -> stat.genCodeWithNewScope(ctx) // ignore context from inner scope
    is Stat.Compose -> stat1.genCode(ctx) + stat2.genCode(ctx)
}

private fun AssignRhs.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is AssignRhs.Expression -> expr.genCode(ctx)
    is AssignRhs.ArrayLiteral -> ctx.takeReg()!!.let { (arrayAddr, innerCtx) -> emptyList<Instruction>() +
            ctx.malloc((exprs.size + 1) * 4) + // Allocate array
            exprs.mapIndexed { i, expr ->
                expr.genCode(innerCtx) + Store(innerCtx.dst!!, arrayAddr, Imm((i + 1) * 4))
            }.flatten() + // Store array values
            Load(innerCtx.dst!!, Imm(exprs.size)) +
            Store(innerCtx.dst!!, arrayAddr) // Store array length
    }
    is AssignRhs.Newpair -> listOf(
            Load(R0, Imm(8)),
            BranchLink(Operand.Label("malloc")),
            Move(ctx.dst!!, R0.op)
    ) + ctx.takeReg()!!.let { (pairReg, ctx2) ->
        listOf((expr1 to null), (expr2 to Imm(4))).flatMap { (expr, offset) ->
            expr.genCode(ctx2) +
                    Load(R0, Imm(4)) +
                    BranchLink(Operand.Label("malloc")) +
                    Store(ctx2.dst!!, R0) +
                    Store(R0, pairReg, offset)
        }
    }
    is AssignRhs.PairElem -> expr.genCode(ctx) +
            Move(R0, ctx.dst!!.op) +
            ctx.branchBuiltin(checkNullPointer) + // Check that the pair isn't null
            Load(ctx.dst!!, ctx.dst!!.op, if (accessor == PairAccessor.FST) null else Imm(4))
    is AssignRhs.Call -> ctx.global.program.funcs.first { it.name == name }.let { func ->
        var totalOffset = 0
        func.params.map(Param::type).zip(args).reversed().flatMap { (type, expr) ->
            expr.genCode(ctx.withStackOffset(totalOffset)) +
                    Store(ctx.dst!!, StackPointer, Imm(type.size), plus = false, moveReg = true).also {
                        totalOffset += type.size
                    } +
                    BranchLink(Operand.Label(func.label)) +
                    Op(Operation.AddOp, StackPointer, StackPointer, Imm(totalOffset))
        }
    }
}

private fun Expr.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Expr.Literal.IntLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt()))) // TODO: int vs long?
    is Expr.Literal.BoolLiteral -> listOf(Move(ctx.dst!!, Imm(if (value) 1 else 0, BOOL)))
    is Expr.Literal.CharLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt(), CHAR)))
    is Expr.Literal.StringLiteral -> listOf(Move(ctx.dst!!, Operand.Label(ctx.global.getStringLabel(value))))
    is Expr.Literal.PairLiteral -> throw IllegalStateException()
    is Expr.Ident -> listOf(Load(ctx.dst!!, StackPointer.op, Imm(ctx.offsetOfIdent(name))))
    is Expr.ArrayElem -> emptyList<Instruction>() +
            Op(Operation.AddOp, ctx.dst!!, StackPointer, Imm(ctx.offsetOfIdent(name.name))) +
            ctx.takeReg()!!.let { (_, ctx2) -> exprs.flatMap { expr ->
                emptyList<Instruction>() +
                        expr.genCode(ctx2) + // evaluate array index
                        Load(ctx.dst!!, ctx.dst!!.op) + // get address of array
                        Move(R0, ctx2.dst!!.op) +
                        Move(GeneralRegister(1), ctx.dst!!.op) +
                        ctx.branchBuiltin(checkArrayBounds) + // check array bounds
                        Op(Operation.AddOp, ctx.dst!!, ctx.dst!!, Imm(4)) +
                        Op(Operation.AddOp, ctx.dst!!, ctx.dst!!, ctx2.dst!!.op,
                                BarrelShift(2, BarrelShift.Type.LSL)) // compute address of desired array elem
            } } + Load(ctx.dst!!, ctx.dst!!.op) // get array elem
    is Expr.UnaryOp -> when (operator) {
        BANG -> expr.genCode(ctx) + Op(Operation.NegateOp, ctx.dst!!, ctx.dst!!, ctx.dst!!.op)
        MINUS -> expr.genCode(ctx) + Op(Operation.RevSubOp, ctx.dst!!, ctx.dst!!, Imm(0))
        LEN -> expr.genCode(ctx) + Load(ctx.dst!!, ctx.dst!!.op)
        ORD, CHR -> expr.genCode(ctx) // Chars and ints should be represented the same way; ignore conversion
    }
    is Expr.BinaryOp -> ctx.takeRegs(2)?.let { (regs, ctx2) ->
        val (dst, nxt) = regs
        if (expr1.weight <= expr2.weight) {
            expr1.genCode(ctx2.withRegs(dst, nxt)) + expr2.genCode(ctx2.withRegs(nxt))
        } else {
            expr2.genCode(ctx2.withRegs(nxt, dst)) + expr2.genCode(ctx2.withRegs(dst))
        } + when (operator) {
            MUL -> emptyList<Instruction>() +
                    LongMul(dst, nxt, dst, nxt) +
                    Compare(nxt, dst.op, BarrelShift(31, BarrelShift.Type.ASR)) +
                    ctx.branchBuiltin(throwOverflowError, Always)
            DIV -> listOf(Op(Operation.DivOp(), dst, dst, nxt.op))
            MOD -> listOf(Op(Operation.ModOp(), dst, dst, nxt.op))
            ADD -> listOf(Op(Operation.AddOp, dst, dst, nxt.op))
            SUB -> listOf(Op(Operation.SubOp, dst, dst, nxt.op))
            GT -> regs.assignBool(SignedGreaterThan)
            GTE -> regs.assignBool(SignedGreaterOrEqual)
            LT -> regs.assignBool(SignedLess)
            LTE -> regs.assignBool(SignedLessOrEqual)
            EQ -> regs.assignBool(Equal)
            NEQ -> regs.assignBool(NotEqual)
            LAND -> listOf(Op(Operation.AndOp, dst, dst, nxt.op))
            LOR -> listOf(Op(Operation.OrOp, dst, dst, nxt.op))
        }
    } ?: TODO()
}

// private fun Stat.genMainFunc(): Function {
//    // TODO remove hardcoded function
//    return Function(Label("main"), listOf(
//            Push(listOf(LinkRegister)),
//            Move(R0, Imm(0)),
//            Pop(listOf(ProgramCounter)),
//            Special.Ltorg
//    ), true)
// }

private fun Pair<Register, Register>.assignBool(cond: Condition) = listOf(
        Compare(first, second.op),
        Move(first, Imm(1, BOOL), cond),
        Move(first, Imm(0, BOOL), cond.inverse)
)

private fun List<Register>.assignBool(cond: Condition) = let { (reg1, reg2) -> (reg1 to reg2).assignBool(cond) }

private fun CodeGenContext.malloc(size: Int): List<Instruction> = listOf(
        Load(R0, Imm(size)),
        BranchLink(Operand.Label("malloc")),
        Move(dst!!, R0.op)
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

private val List<Pair<String, Type>>.offset: Int
    get() = sumBy { it.second.size }

// Generates code for a statement, with instructions to adjust the stack pointer to account for the new scope
private fun Stat.genCodeWithNewScope(ctx: CodeGenContext): List<Instruction> {
    val pre = Op(Operation.SubOp, StackPointer, StackPointer, Imm(vars.offset))
    val post = Op(Operation.AddOp, StackPointer, StackPointer, Imm(vars.offset))
    return emptyList<Instruction>() +
            if (vars.isEmpty()) emptyList() else listOf(pre) +
            genCode(ctx.withNewScope(vars)) +
            if (vars.isEmpty()) emptyList() else listOf(post)
}

val Type.size: Int
    get() = when (this) {
        is Type.BaseType.TypeChar -> 1
        else -> 4
    }

val Func.label: String
    get() = "f_$name"

private fun CodeGenContext.branchBuiltin(f: BuiltinFunction, cond: Condition = Condition.Always): Instruction =
        BranchLink(Operand.Label(f.function.label.name), condition = ).also { global.usedBuiltins.add(f) }

private val Register.op: Operand
    get() = Operand.Reg(this)
