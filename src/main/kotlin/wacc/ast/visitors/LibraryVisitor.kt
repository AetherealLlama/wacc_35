package wacc.ast.visitors

import WaccParserBaseVisitor
import wacc.ast.Program

class LibraryVisitor : WaccParserBaseVisitor<Program>() {
    private val includeVisitor = IncludeVisitor()
    private val classVisitor = ClassVisitor()
    private val functionVisitor = FunctionVisitor()

    override fun visitLibrary(ctx: WaccParser.LibraryContext): Program {
        val includes = ctx.include().map(includeVisitor::visit).toTypedArray()
        val classes = ctx.cls().map(classVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(functionVisitor::visit).toTypedArray()
        return Program(includes, classes, funcs, null)
    }
}
