package wacc.codegen

import java.lang.IllegalStateException
import wacc.ast.*
import wacc.ast.BinaryOperator.*
import wacc.ast.UnaryOperator.*
import wacc.codegen.types.*
import wacc.codegen.types.Condition.*
import wacc.codegen.types.ImmType.*
import wacc.codegen.types.InitializedDatum.InitializedString
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operand.Reg
import wacc.codegen.types.Operation.*
import wacc.codegen.types.Register.*

private const val MIN_USABLE_REG = 4
private const val MAX_USABLE_REG = 11

val usableRegs = (MIN_USABLE_REG..MAX_USABLE_REG).map { GeneralRegister(it) }

private class GlobalCodeGenData(
    val program: Program,
    var strings: List<String> = emptyList(),
    var labelCount: Int = 0
) {
    fun getLabel() = "L${labelCount++}"

    val usedBuiltins: MutableSet<BuiltinFunction> = mutableSetOf()

    fun getStringLabel(s: String): String =
            strings.indexOfFirst { s == it }.let { if (it < 0) strings.size.also { strings += s } else it }
                    .let { "msg_$it" }
}

private class CodeGenContext(
    val global: GlobalCodeGenData,
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
            takeRegs(1)?.let { it.first[0] to it.second }

    fun withNewScope(newScope: List<Pair<String, Type>>): CodeGenContext =
            CodeGenContext(global, stackOffset, listOf(newScope) + scopes, availableRegs)

    fun takeRegs(n: Int): Pair<List<Register>, CodeGenContext>? =
            if (availableRegs.size < n + 2)
                null
            else
                availableRegs.take(n) to CodeGenContext(global, stackOffset, scopes, availableRegs.drop(n))

    fun withRegs(vararg regs: Register) =
            CodeGenContext(global, stackOffset, scopes, regs.asList() + availableRegs)

    fun withStackOffset(offset: Int) =
            CodeGenContext(global, offset, scopes, availableRegs)

    val dst: Register
        get() = availableRegs[0]

    val nxt: Register
        get() = availableRegs[1]
}

fun Program.getAsm(): String {
    val (data, text) = genCode()
    val builder = StringBuilder()
    if (data.data.isNotEmpty())
        builder.appendln(".data")
    data.data.forEach { builder.appendln(it) }
    builder.appendln(".text")
    builder.appendln(".global main")
    text.instructions.flatten().forEach { builder.appendln(it) }
    return builder.toString()
}

private fun Program.genCode(): Pair<Section.DataSection, Section.TextSection> {
    val global = GlobalCodeGenData(this)
    val funcs = funcs.map { it.codeGen(global) }.toMutableList()
    val statCtx = CodeGenContext(global, 0, emptyList())
    funcs += (emptyList<Instruction>() +
            Special.Label("main") +
            Push(LinkRegister) +
            mutableListOf<Instruction>().also { stat.genCodeWithNewScope(statCtx, it) } +
            Load(R0, Imm(0)) +
            Pop(ProgramCounter) +
            Special.Ltorg
            )

    val strings: List<InitializedDatum> = global.strings.map {
        InitializedString(global.getStringLabel(it), it)
    } + global.usedBuiltins.flatMap { it.stringDeps }
            .map { InitializedString(it.first, it.second) }

    global.usedBuiltins.flatMap { it.functionDeps }.forEach { funcs += it.function }

    return Section.DataSection(strings) to Section.TextSection(funcs)
}

private val BuiltinFunction.stringDeps: Set<BuiltinString>
    get() = (deps.second + deps.first.flatMap { it.stringDeps }).toSet()

private val BuiltinFunction.functionDeps: Set<BuiltinFunction>
    get() = (listOf(this) + deps.first.flatMap { it.functionDeps }).toSet()

private fun Func.codeGen(global: GlobalCodeGenData): List<Instruction> {
    val ctx = CodeGenContext(global, 0, emptyList())
    val instrs = mutableListOf<Instruction>()

    instrs.add(Special.Label(label))
    instrs.add(Push(LinkRegister))
    stat.genCodeWithNewScope(ctx, instrs, params.map { it.name to it.type })
    instrs.add(Pop(ProgramCounter))
    instrs.add(Special.Ltorg)

    return instrs
}


// <editor-fold desc="`Stat` code gen">

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

    when(val type = this.type) {
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
private fun Stat.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
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

// </editor-fold>


// <editor-fold desc="`AssignRhs` code gen">

private fun AssignRhs.Expression.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    expr.genCode(ctx, instrs)
}

private fun AssignRhs.ArrayLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    val (arrayAddr, innerCtx) = ctx.takeReg()!!

    ctx.malloc((exprs.size + 1) * 4, instrs)
    for ((i, expr) in exprs.withIndex()) {
        expr.genCode(innerCtx, instrs)
        instrs.add(Store(innerCtx.dst, arrayAddr, Imm((i + 1) * 4)))
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
            instrs.add(BranchLink(Operand.Label(func.label)))
            instrs.add(Op(AddOp, StackPointer, StackPointer, Imm(totalOffset)))
        }
    } else {
        instrs.add(BranchLink(Operand.Label(func.label)))
    }
}

// Delegates code gen to more specific functions
private fun AssignRhs.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is AssignRhs.Expression -> genCode(ctx, instrs)
    is AssignRhs.ArrayLiteral -> genCode(ctx, instrs)
    is AssignRhs.Newpair -> genCode(ctx, instrs)
    is AssignRhs.PairElem -> genCode(ctx, instrs)
    is AssignRhs.Call -> genCode(ctx, instrs)
}

// </editor-fold>


// <editor-fold desc="`Expr` code gen">

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

private fun Expr.Literal.PairLiteral.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) {
    throw IllegalStateException()
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
private fun Expr.genCode(ctx: CodeGenContext, instrs: MutableList<Instruction>) = when (this) {
    is Expr.Literal.IntLiteral -> genCode(ctx, instrs)
    is Expr.Literal.BoolLiteral -> genCode(ctx, instrs)
    is Expr.Literal.CharLiteral -> genCode(ctx, instrs)
    is Expr.Literal.StringLiteral -> genCode(ctx, instrs)
    is Expr.Literal.PairLiteral -> genCode(ctx, instrs)
    is Expr.Ident -> genCode(ctx, instrs)
    is Expr.ArrayElem -> genCode(ctx, instrs)
    is Expr.UnaryOp -> genCode(ctx, instrs)
    is Expr.BinaryOp -> genCode(ctx, instrs)
}

// </editor-fold>


private fun Pair<Register, Register>.assignBool(cond: Condition, instrs: MutableList<Instruction>) {
    instrs.add(Compare(first, second.op))
    instrs.add(Move(first, Imm(1, BOOL), cond))
    instrs.add(Move(first, Imm(0, BOOL), cond.inverse))
}

private fun CodeGenContext.malloc(size: Int, instrs: MutableList<Instruction>) {
    instrs.add(Load(R0, Imm(size)))
    instrs.add(BranchLink(Operand.Label("malloc")))
    moveR0To(dst, instrs)
}

private fun moveR0To(reg: Register, instrs: MutableList<Instruction>){
    if (reg.toString() != R0.toString())
        instrs.add(Move(reg, R0.op))
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

private val List<Pair<String, Type>>.offset: Int
    get() = sumBy { it.second.size }

// Generates code for a statement, with instructions to adjust the stack pointer to account for the new scope
private fun Stat.genCodeWithNewScope(
        ctx: CodeGenContext,
        instrs: MutableList<Instruction>,
        extraVars: List<Pair<String, Type>> = emptyList()
) {
    val vars = this.vars + extraVars

    if (vars.isNotEmpty())
        instrs.add(Op(SubOp, StackPointer, StackPointer, Imm(vars.offset)))
    genCode(ctx.withNewScope(vars), instrs)
    if (vars.isNotEmpty())
        instrs.add(Op(AddOp, StackPointer, StackPointer, Imm(vars.offset)))
}

val Type.size: Int
    get() = when (this) {
        is Type.BaseType.TypeChar -> 1
        else -> 4
    }

val Func.label: String
    get() = "f_$name"

private fun CodeGenContext.branchBuiltin(
        f: BuiltinFunction,
        instrs: MutableList<Instruction>,
        cond: Condition = Always
) {
    instrs.add(BranchLink(f.label, condition = cond))
    global.usedBuiltins.add(f)
}

private val Register.op: Operand
    get() = Reg(this)
