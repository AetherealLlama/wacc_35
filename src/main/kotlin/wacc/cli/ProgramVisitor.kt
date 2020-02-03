package wacc.cli

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Func
import wacc.ast.Program

class ProgramVisitor : WaccParserBaseVisitor<Program>() {
    private val functionVisitor = FunctionVisitor()
    private val statVisitor = StatVisitor()

    override fun visitProgram(ctx: WaccParser.ProgramContext?): Program {
        val funcs = ctx?.func()!!.map(functionVisitor::visit).toTypedArray()
        val stat = statVisitor.visit(ctx.stat())

        return Program(funcs, stat)
    }
}