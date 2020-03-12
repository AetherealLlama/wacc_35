package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import org.koin.core.KoinComponent
import org.koin.core.inject
import wacc.ast.Program

class ProgramVisitor : WaccParserBaseVisitor<Program>(), KoinComponent {
    private val includeVisitor: IncludeVisitor by inject()
    private val classVisitor: ClassVisitor by inject()
    private val functionVisitor: FunctionVisitor by inject()
    private val statVisitor: StatVisitor by inject()

    override fun visitProgram(ctx: WaccParser.ProgramContext): Program {
        val includes = ctx.include().map(includeVisitor::visit).toTypedArray()
        val classes = ctx.cls().map(classVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(functionVisitor::visit).toTypedArray()
        val stat = statVisitor.visit(ctx.stat())
        return Program(includes, classes, funcs, stat)
    }
}
