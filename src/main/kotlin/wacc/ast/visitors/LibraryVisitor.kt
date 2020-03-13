package wacc.ast.visitors

import WaccParserBaseVisitor
import wacc.ast.Program

class LibraryVisitor : WaccParserBaseVisitor<Program>() {
    override fun visitLibrary(ctx: WaccParser.LibraryContext): Program {
        val includes = ctx.include().map(IncludeVisitor::visit).toTypedArray()
        val classes = ctx.cls().map(ClassVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(FunctionVisitor::visit).toTypedArray()
        return Program(includes, classes, funcs, null)
    }
}
