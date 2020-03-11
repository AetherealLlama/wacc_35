package wacc.codegen

import wacc.ast.*
import wacc.ast.Type.BaseType.TypeBool
import wacc.ast.Type.BaseType.TypeChar
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
    val func: Func?,
    val cls: Class?,
    private val scopes: List<Pair<List<Pair<String, Type>>, MutableSet<String>>> = emptyList(),
    private val stackOffset: Int = 0,
    private val availableRegs: List<Register> = usableRegs
) {
    fun offsetOfIdent(ident: String, allowUndefined: Boolean = false): Int {
        var offset = stackOffset
        var found = false
        for ((scope, definedVars) in scopes) {
            for (varData in scope) {
                val (name, memAcc) = varData
                if (found)
                    offset += memAcc.size
                found = found || (name == ident && (ident in definedVars || allowUndefined))
            }
            if (found) {
                break
            }
            offset += scope.sumBy { it.second.size }
        }
        if (found)
            return offset

        // search in func params
        if (func != null) {
            // move past vars in the current scopes, leave space for return address!
            offset = stackOffset + totalScopeOffset + 4
            cls?.let {
                if (ident == "this")
                    return offset
                else
                    offset += Type.ClassType(cls.name).size
            }
            for (param in func.params) {
                if (param.name == ident)
                    return offset
                offset += param.type.size
            }
        }

        throw IllegalStateException()
    }

    fun typeOfIdent(ident: String, allowUndefined: Boolean = false): Type {
        scopes.forEach { (scope, definedVars) ->
            scope.forEach { (name, type) ->
                if (name == ident && (ident in definedVars || allowUndefined))
                    return type
            }
        }
        cls?.let { if (ident == "this") return Type.ClassType(cls.name) }
        func?.params?.forEach { param ->
            if (param.name == ident)
                return param.type
        }
        throw IllegalStateException()
    }

    fun takeReg(): Pair<Register, CodeGenContext>? =
            takeRegs(1)?.let { it.first[0] to it.second }

    fun withNewScope(newScope: List<Pair<String, Type>>): CodeGenContext {
        val newScopes = listOf(newScope to mutableSetOf<String>()) + scopes
        return CodeGenContext(global, func, cls, newScopes, stackOffset, availableRegs)
    }

    fun takeRegs(n: Int, force: Boolean = false): Pair<List<Register>, CodeGenContext>? =
            if (availableRegs.size < n + (if (force) 1 else 2))
                null
            else
                availableRegs.take(n) to CodeGenContext(global, func, cls, scopes, stackOffset, availableRegs.drop(n))

    fun withRegs(vararg regs: Register) =
            CodeGenContext(global, func, cls, scopes, stackOffset, regs.asList() + availableRegs)

    fun withStackOffset(offset: Int) =
            CodeGenContext(global, func, cls, scopes, offset, availableRegs)

    val dst: Register
    get() = availableRegs[0]

    val nxt: Register
    get() = availableRegs[1]

    internal val totalScopeOffset: Int
    get() = scopes.sumBy { (scope, _) -> scope.sumBy { it.second.size } }

    fun defineVar(ident: String) {
        scopes.first().second.add(ident)
    }
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
    val funcs = mutableListOf<List<Instruction>>()
    funcs.addAll(classes.flatMap { cls -> cls.funcs.map { it.genCode(global, cls = cls) } })
    funcs.addAll(this.funcs.map { it.genCode(global, cls = null) })
    val statCtx = CodeGenContext(global, func = null, cls = null)

    // Assemble the top-level statement
    val topLevelStat = emptyList<Instruction>() +
            Special.Label("main") +
            Push(LinkRegister) +
            mutableListOf<Instruction>().also { stat?.genCodeWithNewScope(statCtx, it) ?: TODO() } +
            Load(R0, Imm(0)) +
            Pop(ProgramCounter) +
            Special.Ltorg
    funcs.add(topLevelStat)

    // Collect strings from user code and used builtin functions
    val strings: List<InitializedDatum> = (
            global.strings.map { InitializedString(global.getStringLabel(it), it) } +
            global.usedBuiltins.flatMap { it.stringDeps }.map { InitializedString(it.first, it.second) }
            ).toSet().toList()

    // Collect all dependencies on built-in functions
    funcs.addAll(global.usedBuiltins.flatMap { it.functionDeps }.toSet().map { it.function })

    return Section.DataSection(strings) to Section.TextSection(funcs)
}

private fun Func.genCode(global: GlobalCodeGenData, cls: Class?): List<Instruction> {
    val ctx = CodeGenContext(global, func = this, cls = cls)
    val instrs = mutableListOf<Instruction>()

    instrs.add(Special.Label(label))
    instrs.add(Push(LinkRegister))

    // Skip shifting SP after the function so it can be handled in `return`
    stat.genCodeWithNewScope(ctx, instrs, skipPost = true)

    instrs.add(Pop(ProgramCounter))
    instrs.add(Special.Ltorg)

    return instrs
}

// </editor-fold>

// <editor-fold desc="Helper functions and properties">

internal fun MutableList<Instruction>.opWithConst(op: Operation, constant: Int, rd: Register, rn: Register = rd) {
    var n = constant
    while (n > 1024) {
        add(Op(op, rd, rn, Imm(1024)))
        n -= 1024
    }
    add(Op(op, rd, rn, Imm(n)))
}

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
        instrs.opWithConst(SubOp, vars.offset, StackPointer)
    genCode(ctx.withNewScope(vars), instrs)
    if (!skipPost && vars.isNotEmpty())
        instrs.opWithConst(AddOp, vars.offset, StackPointer)
}

internal val Type.size: Int
    get() = when (this) {
        is TypeChar,
        is TypeBool -> 1
        else -> 4
    }

internal val Type.memAccess: MemoryAccess
    get() = when (this) {
        is TypeChar,
        is TypeBool -> MemoryAccess.Byte
        else -> MemoryAccess.Word
    }

internal val Type.barrelShift: BarrelShift?
    get() = when (this) {
        is TypeChar,
        is TypeBool -> null
        else -> BarrelShift(2, BarrelShift.Type.LSL)
    }

internal val Func.label: String
    get() = (cls?.let { "c_${it.name}_" } ?: "") + "f_${name}_$overloadIx"

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

internal fun CodeGenContext.computeAddressOfArrayElem(
    name: String,
    exprs: Array<Expr>,
    instrs: MutableList<Instruction>
) {
    instrs.opWithConst(AddOp, offsetOfIdent(name), dst, StackPointer)
    for (expr in exprs) {
        takeReg()?.let { (_, ctx2) ->
            expr.genCode(ctx2, instrs) // Register implementation
        } ?: let { // Stack implementation
            instrs.add(Push(listOf(dst))) // save array pointer
            expr.genCode(this, instrs)
            instrs.add(Move(nxt, dst.op)) // nxt = array index
            instrs.add(Pop(listOf(dst))) // dst = array pointer
        } // nxt = array index
        instrs.add(Load(dst, dst.op)) // get address of array
        instrs.add(Move(R0, nxt.op))
        instrs.add(Move(R1, dst.op))
        branchBuiltin(checkArrayBounds, instrs) // check array bounds
        instrs.add(Op(AddOp, dst, dst, Imm(4))) // compute address of desired array elem
        val barrelShift = with(typeOfIdent(name)) {
            if (this is Type.ArrayType) {
                when (this.type) {
                    is TypeChar, TypeBool -> null
                    else -> this.type.barrelShift
                }
            } else
                throw IllegalStateException()
        }
        instrs.add(Op(AddOp, dst, dst, nxt.op, barrelShift))
    }
}

internal fun CodeGenContext.computeAddressOfPairElem(expr: Expr, instrs: MutableList<Instruction>) {
    expr.genCode(this, instrs)
    instrs.add(Move(R0, dst.op))
    branchBuiltin(checkNullPointer, instrs)
}

internal fun CodeGenContext.malloc(size: Int, instrs: MutableList<Instruction>) {
    instrs.add(Load(R0, Imm(size)))
    instrs.add(BranchLink(Operand.Label("malloc")))
    if (dst.toString() != R0.toString())
        instrs.add(Move(dst, R0.op))
}

internal fun Class.offsetOfField(field: String) =
        fields.takeWhile { it.name != field }.sumBy { it.type.size }

internal fun Class.typeOfField(field: String) =
        fields.first { it.name == field }.type

// </editor-fold>
