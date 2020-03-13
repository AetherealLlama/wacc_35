package wacc.ast.visitors

import WaccParserBaseVisitor
import wacc.ast.Include
import wacc.ast.pos

object IncludeVisitor : WaccParserBaseVisitor<Include>() {
    override fun visitInclude(ctx: WaccParser.IncludeContext): Include {
        val filename = ctx.FILENAME().text
        return Include(ctx.pos, filename)
    }
}
