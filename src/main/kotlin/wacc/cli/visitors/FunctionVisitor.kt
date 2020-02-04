package wacc.cli.visitors

import WaccParserBaseVisitor
import wacc.ast.Func
import wacc.ast.Param
import wacc.ast.getTypeFromContext

class FunctionVisitor : WaccParserBaseVisitor<Func>() {
    private val statVisitor = StatVisitor()

    override fun visitFunc(ctx: WaccParser.FuncContext?): Func {
        val type = getTypeFromContext(ctx?.type()!!)
        val name = ctx.IDENT().text
        val params = getParamsFromParamListContext(ctx.paramList())
        val stat = statVisitor.visit(ctx.stat())
        return Func(type, name, params, stat)
    }
}

private fun getParamsFromParamListContext(ctx: WaccParser.ParamListContext?): Array<Param> {
    return ctx?.param()?.map(::getParamFromParamContext)?.toTypedArray() ?: emptyArray()
}

private fun getParamFromParamContext(ctx: WaccParser.ParamContext): Param {
    val type = getTypeFromContext(ctx.type())
    val name = ctx.IDENT().text
    return Param(type, name)
}