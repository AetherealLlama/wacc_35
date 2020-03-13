package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Class
import wacc.ast.Field
import wacc.ast.pos

object ClassVisitor : WaccParserBaseVisitor<Class>() {
    override fun visitCls(ctx: WaccParser.ClsContext): Class {
        val name = ctx.IDENT().text
        val fields = ctx.param().map {
            val type = TypeVisitor.visit(it.type())
            val name = it.IDENT().text
            Field(it.pos, type, name)
        }
        val funcs = ctx.func().map(FunctionVisitor::visit)
        return Class(ctx.pos, name, fields, funcs)
    }
}
