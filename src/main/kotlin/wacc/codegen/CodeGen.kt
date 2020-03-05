package wacc.codegen

import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Stat
import wacc.ast.Type
import wacc.codegen.types.*
import wacc.codegen.types.Condition.Always
import wacc.codegen.types.InitializedDatum.InitializedString
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operand.Reg
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.Operation.SubOp
import wacc.codegen.types.Register.*

// <editor-fold desc="Constants">

private const val MIN_USABLE_REG = 4
private const val MAX_USABLE_REG = 11

private val usableRegs = (MIN_USABLE_REG..MAX_USABLE_REG).map { GeneralRegister(it) }

// </editor-fold>

// <editor-fold desc="Context Classes">

internal class GlobalCodeGenData(
    val program: Program,
    var strings: List<String> = emptyList(),
    private var labelCount: Int = 0
) {
    fun getLabel() = "L${labelCount++}"

    val usedBuiltins: MutableSet<BuiltinFunction> = mutableSetOf()

    fun getStringLabel(s: String): String =
            strings.indexOfFirst { s == it }.let { if (it < 0) strings.size.also { strings += s } else it }
                    .let { "msg_$it" }
}

internal class CodeGenContext(
    val global: GlobalCodeGenData,
    private val stackOffset: Int,
    private val scopes: List<List<Pair<String, Type>>>,
    private val availableRegs: List<Register> = usableRegs
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

    internal val totalScopeOffset: Int
        get() = scopes.sumBy { it.sumBy { it.second.size } }
}

// </editor-fold>

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

// <editor-fold desc="Code Generation">

private fun Program.genCode(): Pair<Section.DataSection, Section.TextSection> {
    val global = GlobalCodeGenData(this)
    val funcs = funcs.map { it.genCode(global) }.toMutableList()
    val statCtx = CodeGenContext(global, 0, emptyList())

    // Assemble the top-level statement
    val topLevelStat = emptyList<Instruction>() +
            Special.Label("main") +
            Push(LinkRegister) +
            mutableListOf<Instruction>().also { stat.genCodeWithNewScope(statCtx, it) } +
            Load(R0, Imm(0)) +
            Pop(ProgramCounter) +
            Special.Ltorg
    funcs.add(topLevelStat)

    // Collect strings from user code and used builtin functions
    val strings: List<InitializedDatum> = global.strings.map { InitializedString(global.getStringLabel(it), it) } +
            global.usedBuiltins.flatMap { it.stringDeps }.map { InitializedString(it.first, it.second) }

    // Collect all dependencies on built-in functions
    funcs.addAll(global.usedBuiltins.flatMap { it.functionDeps }.toSet().map { it.function })

    return Section.DataSection(strings) to Section.TextSection(funcs)
}

private fun Func.genCode(global: GlobalCodeGenData): List<Instruction> {
    val ctx = CodeGenContext(global, 0, emptyList())
    val instrs = mutableListOf<Instruction>()

    instrs.add(Special.Label(label))
    instrs.add(Push(LinkRegister))

    // Skip shifting SP after the function so it can be handled in `return`
    stat.genCodeWithNewScope(ctx, instrs, params.map { it.name to it.type }, skipPost = true)

    instrs.add(Pop(ProgramCounter))
    instrs.add(Special.Ltorg)

    return instrs
}

// </editor-fold>

// <editor-fold desc="Helper functions and properties">

private val BuiltinFunction.stringDeps: Set<BuiltinString>
    get() = (deps.second + deps.first.flatMap { it.stringDeps }).toSet()

private val BuiltinFunction.functionDeps: Set<BuiltinFunction>
    get() = (listOf(this) + deps.first.flatMap { it.functionDeps }).toSet()

private val List<Pair<String, Type>>.offset: Int
    get() = sumBy { it.second.size }

// Generates code for a statement, with instructions to adjust the stack pointer to account for the new scope
internal fun Stat.genCodeWithNewScope(
    ctx: CodeGenContext,
    instrs: MutableList<Instruction>,
    extraVars: List<Pair<String, Type>> = emptyList(),
    skipPost: Boolean = false
) {
    val vars = this.vars + extraVars

    if (vars.isNotEmpty())
        instrs.add(Op(SubOp, StackPointer, StackPointer, Imm(vars.offset)))
    genCode(ctx.withNewScope(vars), instrs)
    if (!skipPost && vars.isNotEmpty())
        instrs.add(Op(AddOp, StackPointer, StackPointer, Imm(vars.offset)))
}

internal val Type.size: Int
    get() = when (this) {
        is Type.BaseType.TypeChar -> 1
        else -> 4
    }

internal val Func.label: String
    get() = "f_$name"

internal fun CodeGenContext.branchBuiltin(
    f: BuiltinFunction,
    instrs: MutableList<Instruction>,
    cond: Condition = Always
) {
    instrs.add(BranchLink(f.label, condition = cond))
    global.usedBuiltins.add(f)
}

internal val Register.op: Operand
    get() = Reg(this)

// </editor-fold>
