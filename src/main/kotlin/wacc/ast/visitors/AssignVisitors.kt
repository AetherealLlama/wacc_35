package wacc.ast.visitors

import WaccParser
import WaccParserBaseVisitor
import wacc.ast.*

class AssignLhsVisitor : WaccParserBaseVisitor<AssignLhs>() {
    private val exprVisitor = ExprVisitor()

    override fun visitAssignLhsVariable(ctx: WaccParser.AssignLhsVariableContext?): AssignLhs {
        val name = ctx?.IDENT()?.text!!
        return AssignLhs.Variable(ctx.pos, name)
    }

    override fun visitAssignLhsArrayElem(ctx: WaccParser.AssignLhsArrayElemContext?): AssignLhs {
        val arrayElemCtx = ctx?.arrayElem()
        val name = arrayElemCtx?.IDENT()?.text!!
        val exprs = arrayElemCtx.expr()?.map(exprVisitor::visit)?.toTypedArray()!!
        return AssignLhs.ArrayElem(ctx.pos, name, exprs)
    }

    override fun visitAssignLhsPairElem(ctx: WaccParser.AssignLhsPairElemContext?): AssignLhs {
        val pair = getPairElemFromPairElemContext(ctx?.pairElem()!!)
        return AssignLhs.PairElem(ctx.pos, pair.first, pair.second)
    }
}

class AssignRhsVisitor : WaccParserBaseVisitor<AssignRhs>() {
    private val exprVisitor = ExprVisitor()

    override fun visitAssignRhsExpr(ctx: WaccParser.AssignRhsExprContext?): AssignRhs {
        val expr = exprVisitor.visit(ctx?.expr())
        return AssignRhs.Expression(ctx!!.pos, expr)
    }

    override fun visitAssignRhsArrayLiter(ctx: WaccParser.AssignRhsArrayLiterContext?): AssignRhs {
        val exprs = ctx?.arrayLiter()?.expr()?.map(exprVisitor::visit)?.toTypedArray()!!
        return AssignRhs.ArrayLiteral(ctx.pos, exprs)
    }

    override fun visitAssignRhsNewpair(ctx: WaccParser.AssignRhsNewpairContext?): AssignRhs {
        val expr1 = exprVisitor.visit(ctx?.expr(0))
        val expr2 = exprVisitor.visit(ctx?.expr(1))
        return AssignRhs.Newpair(ctx!!.pos, expr1, expr2)
    }

    override fun visitAssignRhsPairElem(ctx: WaccParser.AssignRhsPairElemContext?): AssignRhs {
        val pair = getPairElemFromPairElemContext(ctx?.pairElem()!!)
        return AssignRhs.PairElem(ctx.pos, pair.first, pair.second)
    }

    override fun visitAssignRhsCall(ctx: WaccParser.AssignRhsCallContext?): AssignRhs {
        val name = ctx?.IDENT()?.text!!
        val args = ctx.argList()?.expr()?.map(exprVisitor::visit)?.toTypedArray() ?: emptyArray()
        return AssignRhs.Call(ctx.pos, name, args)
    }

    override fun visitAssignRhsNewInstance(ctx: WaccParser.AssignRhsNewInstanceContext?): AssignRhs {
        return TODO()
    }
}

private fun getPairElemFromPairElemContext(ctx: WaccParser.PairElemContext): Pair<PairAccessor, Expr> {
    val accessor = if (ctx.acc.type == WaccParser.FST) PairAccessor.FST else PairAccessor.SND
    val expr = ExprVisitor().visit(ctx.expr())
    return Pair(accessor, expr)
}
