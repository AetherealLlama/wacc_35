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
    text.instructions.flatten().forEach { builder.appendln(it) }
    return builder.toString()
}

private fun Program.genCode(): Pair<Section.DataSection, Section.TextSection> {
    val global = GlobalCodeGenData(this)
    val funcs = funcs.map { it.codeGen(global) }.toMutableList()
    val statCtx = CodeGenContext(global, 0, emptyList())
    funcs += (emptyList<Instruction>() +
            Special.Global("main") +
            Special.Label("main") +
            Push(LinkRegister) +
            stat.genCodeWithNewScope(statCtx) +
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
    return emptyList<Instruction>() +
            Special.Label(label) +
            Push(LinkRegister) +
            stat.genCodeWithNewScope(ctx, params.map { it.name to it.type }) +
            Pop(ProgramCounter) +
            Special.Ltorg
}

private fun Stat.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Stat.Skip -> emptyList()
    is Stat.AssignNew -> rhs.genCode(ctx) + Store(ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(name)))
    is Stat.Assign -> rhs.genCode(ctx).let { rhsInstrs ->
        when (lhs) {
            is AssignLhs.Variable -> rhsInstrs + Store(ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(lhs.name)))
            is AssignLhs.ArrayElem -> TODO()
            is AssignLhs.PairElem -> TODO()
        }
    }
    is Stat.Read -> listOf(Op(AddOp, ctx.dst, StackPointer, Imm(0)), Move(R0, ctx.dst.op)) + when (type) {
        is Type.BaseType.TypeInt -> ctx.branchBuiltin(readInt)
        is Type.BaseType.TypeChar -> ctx.branchBuiltin(readChar)
        else -> throw IllegalStateException()
    } + Load(ctx.dst, StackPointer.op)
    is Stat.Free -> expr.genCode(ctx) + Move(R0, ctx.dst.op) + ctx.branchBuiltin(freePair)
    is Stat.Return -> expr.genCode(ctx) + Move(R0, ctx.dst.op) + Pop(listOf(ProgramCounter))
    is Stat.Exit ->
        expr.genCode(ctx) + Move(R0, ctx.dst.op) + BranchLink(Operand.Label("exit"))
    is Stat.Print -> expr.genCode(ctx) + Move(R0, ctx.dst.op) + when (type) {
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
    is Stat.Println -> Stat.Print(pos, expr).also { it.type = type }.genCode(ctx) + ctx.branchBuiltin(printLn)
    is Stat.IfThenElse -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) ->
        emptyList<Instruction>() +
                expr.genCode(ctx) + // condition
                Compare(ctx.dst, Imm(0)) +
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
                Compare(ctx.dst, Imm(1)) +
                Branch(Operand.Label(label1), Equal)
    }
    is Stat.Begin -> stat.genCodeWithNewScope(ctx) // ignore context from inner scope
    is Stat.Compose -> stat1.genCode(ctx) + stat2.genCode(ctx)
}

private fun AssignRhs.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is AssignRhs.Expression -> expr.genCode(ctx)
    is AssignRhs.ArrayLiteral -> ctx.takeReg()!!.let { (arrayAddr, innerCtx) ->
        emptyList<Instruction>() +
                ctx.malloc((exprs.size + 1) * 4) + // Allocate array
                exprs.mapIndexed { i, expr ->
                    expr.genCode(innerCtx) + Store(innerCtx.dst, arrayAddr, Imm((i + 1) * 4))
                }.flatten() + // Store array values
                Load(innerCtx.dst, Imm(exprs.size)) +
                Store(innerCtx.dst, arrayAddr) // Store array length
    }
    is AssignRhs.Newpair -> listOf(
            Load(R0, Imm(8)),
            BranchLink(Operand.Label("malloc")),
            Move(ctx.dst, R0.op)
    ) + ctx.takeReg()!!.let { (pairReg, ctx2) ->
        listOf((expr1 to null), (expr2 to Imm(4))).flatMap { (expr, offset) ->
            expr.genCode(ctx2) +
                    Load(R0, Imm(4)) +
                    BranchLink(Operand.Label("malloc")) +
                    Store(ctx2.dst, R0) +
                    Store(R0, pairReg, offset)
        }
    }
    is AssignRhs.PairElem -> expr.genCode(ctx) +
            moveR0(ctx.dst) +
            ctx.branchBuiltin(checkNullPointer) + // Check that the pair isn't null
            Load(ctx.dst, ctx.dst.op, if (accessor == PairAccessor.FST) null else Imm(4))
    is AssignRhs.Call -> ctx.global.program.funcs.first { it.name == name }.let { func ->
        var totalOffset = 0
        if (func.params.isNotEmpty()) {
            func.params.map(Param::type).zip(args).reversed().flatMap { (type, expr) ->
                expr.genCode(ctx.withStackOffset(totalOffset)) +
                        Store(ctx.dst, StackPointer, Imm(type.size), plus = false, moveReg = true).also {
                            totalOffset += type.size
                        } +
                        BranchLink(Operand.Label(func.label)) +
                        Op(AddOp, StackPointer, StackPointer, Imm(totalOffset))
            }
        } else {
            listOf(BranchLink(Operand.Label(func.label)))
        }
    }
}

private fun Expr.genCode(ctx: CodeGenContext): List<Instruction> {
    return when (this) {
        is Expr.Literal.IntLiteral ->
            if (value in 0..255) listOf(Move(ctx.dst, Imm(value.toInt())))
            else listOf(Load(ctx.dst, Imm(value.toInt())))
        is Expr.Literal.BoolLiteral -> listOf(Move(ctx.dst, Imm(if (value) 1 else 0, BOOL)))
        is Expr.Literal.CharLiteral -> listOf(Move(ctx.dst, Imm(value.toInt(), CHAR)))
        is Expr.Literal.StringLiteral -> listOf(Load(ctx.dst, Operand.Label(ctx.global.getStringLabel(value))))
        is Expr.Literal.PairLiteral -> throw IllegalStateException()
        is Expr.Ident -> listOf(Load(ctx.dst, StackPointer.op, Imm(ctx.offsetOfIdent(name))))
        is Expr.ArrayElem -> emptyList<Instruction>() +
                Op(AddOp, ctx.dst, StackPointer, Imm(ctx.offsetOfIdent(name.name))) + // get variable address
                exprs.flatMap { expr ->
                    emptyList<Instruction>() +
                            (ctx.takeReg()?.let { (_, ctx2) ->
                                expr.genCode(ctx2) // Register implementation
                            } ?: let {
                                emptyList<Instruction>() + // Stack implementation
                                        Push(listOf(ctx.dst)) + // save array pointer
                                        expr.genCode(ctx) +
                                        Move(ctx.nxt, ctx.dst.op) + // nxt = array index
                                        Pop(listOf(ctx.dst)) // dst = array pointer
                            }) + // nxt = array index
                            Load(ctx.dst, ctx.dst.op) + // get address of array
                            Move(R0, ctx.nxt.op) +
                            Move(R1, ctx.dst.op) +
                            ctx.branchBuiltin(checkArrayBounds) + // check array bounds
                            Op(AddOp, ctx.dst, ctx.dst, Imm(4)) + // compute address of desired array elem
                            Op(AddOp, ctx.dst, ctx.dst, ctx.nxt.op, BarrelShift(2, BarrelShift.Type.LSL))
                } + Load(ctx.dst, ctx.dst.op) // get array elem
        is Expr.UnaryOp -> when (operator) {
            BANG -> expr.genCode(ctx) + Op(NegateOp, ctx.dst, ctx.dst, ctx.dst.op)
            MINUS -> expr.genCode(ctx) + Op(RevSubOp, ctx.dst, ctx.dst, Imm(0))
            LEN -> expr.genCode(ctx) + Load(ctx.dst, ctx.dst.op)
            ORD, CHR -> expr.genCode(ctx) // Chars and ints should be represented the same way; ignore conversion
        }
        is Expr.BinaryOp -> {
            val instrs = ctx.takeRegs(2)?.let { (_, ctx2) -> // Register implementation
                if (expr1.weight <= expr2.weight) {
                    expr1.genCode(ctx2.withRegs(ctx.dst, ctx.nxt)) + expr2.genCode(ctx2.withRegs(ctx.nxt))
                } else {
                    expr2.genCode(ctx2.withRegs(ctx.nxt, ctx.dst)) + expr2.genCode(ctx2.withRegs(ctx.dst))
                }
            } ?: (emptyList<Instruction>() + // Stack implementation
                    expr1.genCode(ctx) +
                    Push(listOf(ctx.dst)) +
                    expr2.genCode(ctx) +
                    Move(ctx.nxt, ctx.dst.op) +
                    Pop(listOf(ctx.dst))
                    )
            val regs = ctx.dst to ctx.nxt
            instrs + when (operator) {
                MUL -> emptyList<Instruction>() +
                        LongMul(ctx.dst, ctx.nxt, ctx.dst, ctx.nxt) +
                        Compare(ctx.nxt, ctx.dst.op, BarrelShift(31, BarrelShift.Type.ASR)) +
                        ctx.branchBuiltin(throwOverflowError, Always)
                DIV -> listOf(Op(DivOp(), ctx.dst, ctx.dst, ctx.nxt.op))
                MOD -> listOf(Op(ModOp(), ctx.dst, ctx.dst, ctx.nxt.op))
                ADD -> emptyList<Instruction>() +
                        Op(AddOp, ctx.dst, ctx.dst, ctx.nxt.op, setCondCodes = true) +
                        ctx.branchBuiltin(throwOverflowError, Overflow)
                SUB -> listOf(Op(SubOp, ctx.dst, ctx.dst, ctx.nxt.op))
                GT -> regs.assignBool(SignedGreaterThan)
                GTE -> regs.assignBool(SignedGreaterOrEqual)
                LT -> regs.assignBool(SignedLess)
                LTE -> regs.assignBool(SignedLessOrEqual)
                EQ -> regs.assignBool(Equal)
                NEQ -> regs.assignBool(NotEqual)
                LAND -> listOf(Op(AndOp, ctx.dst, ctx.dst, ctx.nxt.op))
                LOR -> listOf(Op(OrOp, ctx.dst, ctx.dst, ctx.nxt.op))
            }
        }
    }
}

private fun Pair<Register, Register>.assignBool(cond: Condition) = listOf(
        Compare(first, second.op),
        Move(first, Imm(1, BOOL), cond),
        Move(first, Imm(0, BOOL), cond.inverse)
)

private fun CodeGenContext.malloc(size: Int): List<Instruction> = listOf(
        Load(R0, Imm(size)),
        BranchLink(Operand.Label("malloc"))
) + moveR0(dst)

private fun moveR0(reg: Register): List<Instruction> =
        if (reg.toString() == R0.toString()) emptyList() else listOf(Move(reg, R0.op))

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
private fun Stat.genCodeWithNewScope(ctx: CodeGenContext, extraVars: List<Pair<String, Type>> = emptyList()): List<Instruction> {
    val vars = this.vars + extraVars
    val pre = Op(SubOp, StackPointer, StackPointer, Imm(vars.offset))
    val post = Op(AddOp, StackPointer, StackPointer, Imm(vars.offset))
    return emptyList<Instruction>() +
            (if (vars.isEmpty()) emptyList() else listOf(pre)) +
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

private fun CodeGenContext.branchBuiltin(f: BuiltinFunction, cond: Condition = Always): Instruction =
        BranchLink(f.label, condition = cond).also { global.usedBuiltins.add(f) }

private val Register.op: Operand
    get() = Reg(this)
