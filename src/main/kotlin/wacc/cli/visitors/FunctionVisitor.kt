package wacc.cli.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Func
import wacc.ast.Param

class FunctionVisitor : WaccParserBaseVisitor<Func>() {
    private val statVisitor = StatVisitor()
    private val typeVisitor = TypeVisitor()

    override fun visitFunc(ctx: WaccParser.FuncContext?): Func {
        val type = typeVisitor.visit(ctx?.type())
        val name = ctx?.IDENT()?.text ?: ""
        val params = getParamsFromParamListContext(ctx?.paramList())
        val stat = statVisitor.visit(ctx?.stat())
        return Func(type, name, params, stat)
    }

    private fun getParamsFromParamListContext(ctx: WaccParser.ParamListContext?): Array<Param> {
        return ctx?.param()?.map(::getParamFromParamContext)?.toTypedArray() ?: emptyArray()
    }

    private fun getParamFromParamContext(ctx: WaccParser.ParamContext): Param {
        val type = typeVisitor.visit(ctx.type())
        val name = ctx.IDENT().text
        return Param(type, name)
    }
}
