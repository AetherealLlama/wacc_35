package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.Func
import wacc.ast.Param
import wacc.ast.pos

object FunctionVisitor : WaccParserBaseVisitor<Func>() {
    override fun visitFunc(ctx: WaccParser.FuncContext?): Func {
        val type = TypeVisitor.visit(ctx?.type())
        val name = ctx?.IDENT()?.text ?: ""
        val params = getParamsFromParamListContext(ctx?.paramList())
        val stat = StatVisitor.visit(ctx?.stat())
        return Func(ctx!!.pos, type, name, params, stat)
    }

    private fun getParamsFromParamListContext(ctx: WaccParser.ParamListContext?): Array<Param> {
        return ctx?.param()?.map(::getParamFromParamContext)?.toTypedArray() ?: emptyArray()
    }

    private fun getParamFromParamContext(ctx: WaccParser.ParamContext): Param {
        val type = TypeVisitor.visit(ctx.type())
        val name = ctx.IDENT().text
        return Param(ctx.pos, type, name)
    }
}
