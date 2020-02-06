package wacc.cli.visitors

import WaccParserBaseVisitor
import wacc.ast.BinaryOperator
import wacc.ast.Expr
import wacc.ast.UnaryOperator
import java.lang.IllegalStateException

class ExprVisitor : WaccParserBaseVisitor<Expr>() {
    override fun visitInt(ctx: WaccParser.IntContext?): Expr {
        ctx?.integer()?.let { int ->
            var num = int.INTLITER().text.toLong()
            if (int.sign?.type == WaccLexer.MINUS) num = -num
            return Expr.Literal.IntLiteral(num)
        }
        throw IllegalStateException()
    }

    override fun visitLiteral(ctx: WaccParser.LiteralContext?): Expr {
        when (ctx?.lit?.type) {
            WaccLexer.BOOLLITER ->
                return Expr.Literal.BoolLiteral(ctx.lit.text == "true")
            WaccLexer.CHARLITER -> return Expr.Literal.CharLiteral(ctx.lit.text[0])
            WaccLexer.STRLITER -> return Expr.Literal.StringLiteral(ctx.lit.text)
            WaccLexer.PAIRLITER -> return Expr.Literal.PairLiteral
        }
        // TODO: find a better default return value, this should never run but the compilers asks
        // for it
        return Expr.Literal.IntLiteral(0)
    }

    override fun visitIdExpr(ctx: WaccParser.IdExprContext?): Expr {
        val name = ctx?.IDENT()?.text!!
        return Expr.Ident(name)
    }

    override fun visitArrayElemExpr(ctx: WaccParser.ArrayElemExprContext?): Expr {
        val arrayElemCtx = ctx?.arrayElem()
        val name = arrayElemCtx?.IDENT()?.text!!
        val exprs = arrayElemCtx.expr().map(this::visit).toTypedArray()
        return Expr.ArrayElem(Expr.Ident(name), exprs)
    }

    override fun visitUnaryOpExpr(ctx: WaccParser.UnaryOpExprContext?): Expr {
        val operator = when (ctx?.op?.type) {
            WaccLexer.BANG -> UnaryOperator.BANG
            WaccLexer.MINUS -> UnaryOperator.MINUS
            WaccLexer.LEN -> UnaryOperator.LEN
            WaccLexer.ORD -> UnaryOperator.ORD
            WaccLexer.CHR -> UnaryOperator.CHR
            else -> UnaryOperator.BANG
        }
        val expr = visit(ctx?.expr())
        return Expr.UnaryOp(operator, expr)
    }

    override fun visitBinaryOpExpr(ctx: WaccParser.BinaryOpExprContext?): Expr {
        val operator = when (ctx?.op?.type) {
            WaccLexer.MUL -> BinaryOperator.MUL
            WaccLexer.DIV -> BinaryOperator.DIV
            WaccLexer.MOD -> BinaryOperator.MOD
            WaccLexer.PLUS -> BinaryOperator.ADD
            WaccLexer.MINUS -> BinaryOperator.SUB
            WaccLexer.GT -> BinaryOperator.GT
            WaccLexer.GTE -> BinaryOperator.GTE
            WaccLexer.LT -> BinaryOperator.LT
            WaccLexer.LTE -> BinaryOperator.LTE
            WaccLexer.EQ -> BinaryOperator.EQ
            WaccLexer.NEQ -> BinaryOperator.NEQ
            WaccLexer.LAND -> BinaryOperator.LAND
            WaccLexer.LOR -> BinaryOperator.LOR
            else -> BinaryOperator.MUL
        }
        val expr1 = visit(ctx?.expr(0))
        val expr2 = visit(ctx?.expr(1))
        return Expr.BinaryOp(operator, expr1, expr2)
    }

    override fun visitParensExpr(ctx: WaccParser.ParensExprContext?): Expr {
        return visit(ctx?.expr())
    }
}