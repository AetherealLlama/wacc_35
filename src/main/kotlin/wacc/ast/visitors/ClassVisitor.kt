package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import org.koin.core.KoinComponent
import org.koin.core.inject
import wacc.ast.Class
import wacc.ast.Field
import wacc.ast.pos

class ClassVisitor : WaccParserBaseVisitor<Class>(), KoinComponent {
    private val typeVisitor: TypeVisitor by inject()
    private val functionVisitor: FunctionVisitor by inject()

    override fun visitCls(ctx: WaccParser.ClsContext): Class {
        val name = ctx.IDENT().text
        val fields = ctx.param().map {
            val type = typeVisitor.visit(it.type())
            val name = it.IDENT().text
            Field(it.pos, type, name)
        }
        val funcs = ctx.func().map(functionVisitor::visit)
        return Class(ctx.pos, name, fields, funcs)
    }
}
