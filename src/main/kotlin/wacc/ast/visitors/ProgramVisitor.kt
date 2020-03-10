package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Program

class ProgramVisitor : WaccParserBaseVisitor<Program>() {
    private val classVisitor = ClassVisitor()
    private val functionVisitor = FunctionVisitor()
    private val statVisitor = StatVisitor()

    override fun visitProgram(ctx: WaccParser.ProgramContext): Program {
        val classes = ctx.cls().map(classVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(functionVisitor::visit).toTypedArray()
        val stat = statVisitor.visit(ctx.stat())
        return Program(classes, funcs, stat)
    }
}
