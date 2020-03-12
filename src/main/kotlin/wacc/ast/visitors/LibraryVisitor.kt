package wacc.ast.visitors

import WaccParserBaseVisitor
import org.koin.core.KoinComponent
import org.koin.core.inject
import wacc.ast.Program

class LibraryVisitor : WaccParserBaseVisitor<Program>(), KoinComponent {
    private val includeVisitor: IncludeVisitor by inject()
    private val classVisitor: ClassVisitor by inject()
    private val functionVisitor: FunctionVisitor by inject()

    override fun visitLibrary(ctx: WaccParser.LibraryContext): Program {
        val includes = ctx.include().map(includeVisitor::visit).toTypedArray()
        val classes = ctx.cls().map(classVisitor::visit).toTypedArray()
        val funcs = ctx.func().map(functionVisitor::visit).toTypedArray()
        return Program(includes, classes, funcs, null)
    }
}
