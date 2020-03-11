package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Class
import wacc.ast.Field
import wacc.ast.pos

class ClassVisitor : WaccParserBaseVisitor<Class>() {
    private val typeVisitor = TypeVisitor()
    private val functionvisitor = FunctionVisitor()

    override fun visitCls(ctx: WaccParser.ClsContext): Class {
        val name = ctx.IDENT().text
        val fields = ctx.param().map {
            val type = typeVisitor.visit(it.type())
            val name = it.IDENT().text
            Field(it.pos, type, name)
        }
        val funcs = ctx.func().map(functionvisitor::visit)
        return Class(ctx.pos, name, fields, funcs)
    }
}
