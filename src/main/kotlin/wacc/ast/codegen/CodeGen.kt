package wacc.ast.codegen

import wacc.ast.Expr
import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Stat
import wacc.ast.codegen.types.*
import wacc.ast.codegen.types.Condition.Always
import wacc.ast.codegen.types.Function
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Register.*


private class CodeGenContext(val func: Func?, val vars: Map<String, Int>) {
    fun resolveIdent(ident: String): Int? =
            func?.params?.indexOfFirst { it.name == ident }?.let { if (it < 0) null else (it+1)*4 }
                    ?: vars[ident]

    fun withNewVar(name: String) = CodeGenContext(func, vars + (name to TODO()))
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
    val dataSection = Section.DataSection(emptyList())
    val funcs = funcs.map(Func::codeGen).toMutableList()
    funcs += stat.genMainFunc()
    return Section.DataSection(emptyList()) to Section.TextSection(funcs)
}

private fun Func.codeGen(): Function {
    return Function(Label(name), emptyList(), false)
}

private fun Expr.genCode(ctx: CodeGenContext): Pair<List<Instruction>, Operand> = when (this) {
    is Expr.Literal.IntLiteral -> emptyList<Instruction>() to Imm(value.toInt(), ImmType.INT)  // TODO: int vs long?
    is Expr.Literal.BoolLiteral -> emptyList<Instruction>() to Imm(if (value) 1 else 0, ImmType.BOOL)
    is Expr.Literal.CharLiteral -> emptyList<Instruction>() to Imm(value.toInt(), ImmType.CHAR)
    is Expr.Literal.StringLiteral -> emptyList<Instruction>() to Operand.Label(TODO())
    is Expr.Literal.PairLiteral -> TODO()
    is Expr.Ident -> TODO()
    is Expr.ArrayElem -> TODO()
    is Expr.UnaryOp -> TODO()
    is Expr.BinaryOp -> TODO()
}

private fun Stat.genMainFunc(): Function {
    // TODO remove hardcoded function
    return Function(Label("main"), listOf(
            Push(listOf(LinkRegister)),
            Move(GeneralRegister(0), Imm(0, ImmType.INT)),
            Pop(listOf(ProgramCounter)),
            Special.Ltorg
    ), true)
}
