package wacc.ast.codegen

import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Stat
import wacc.ast.codegen.types.Condition.Always
import wacc.ast.codegen.types.Function
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Label
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Register.*
import wacc.ast.codegen.types.Section

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

private fun Stat.genMainFunc(): Function {
    // TODO remove hardcoded function
    return Function(Label("main"), listOf(
            Push(listOf(LinkRegister)),
            Move(GeneralRegister(0), Imm(0)),
            Pop(listOf(ProgramCounter)),
            Special.Ltorg
    ), true)
}
