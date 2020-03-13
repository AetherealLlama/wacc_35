package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Program

object ProgramVisitor : WaccParserBaseVisitor<Program>() {
    override fun visitProgram(ctx: WaccParser.ProgramContext): Program {
        val includes = ctx.include().map(IncludeVisitor::visit).toTypedArray()
        val classes = ctx.cls().map(ClassVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(FunctionVisitor::visit).toTypedArray()
        val stat = StatVisitor.visit(ctx.stat())
        return Program(includes, classes, funcs, stat)
    }
}
