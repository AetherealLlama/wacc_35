package wacc.cli.visitors

import WaccParserBaseVisitor
import wacc.ast.Stat

class StatVisitor : WaccParserBaseVisitor<Stat>() {
    private val exprVisitor = ExprVisitor()
    private val typeVisitor = TypeVisitor()
    private val assignRhsVisitor = AssignRhsVisitor()
    private val assignLhsVisitor = AssignLhsVisitor()

    override fun visitSkip(ctx: WaccParser.SkipContext?): Stat = Stat.Skip

    override fun visitAssignNew(ctx: WaccParser.AssignNewContext?): Stat {
        val type = typeVisitor.visit(ctx?.type())
        val name = ctx?.IDENT()?.text!!
        val rhs = assignRhsVisitor.visit(ctx.assignRhs())
        return Stat.AssignNew(type, name, rhs)
    }

    override fun visitAssign(ctx: WaccParser.AssignContext?): Stat {
        val lhs = assignLhsVisitor.visit(ctx?.assignLhs())
        val rhs = assignRhsVisitor.visit(ctx?.assignRhs())
        return Stat.Assign(lhs, rhs)
    }

    override fun visitRead(ctx: WaccParser.ReadContext?): Stat {
        val lhs = assignLhsVisitor.visit(ctx?.assignLhs())
        return Stat.Read(lhs)
    }

    override fun visitFree(ctx: WaccParser.FreeContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        return Stat.Free(expr)
    }

    override fun visitReturn(ctx: WaccParser.ReturnContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        return Stat.Return(expr)
    }

    override fun visitExit(ctx: WaccParser.ExitContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        return Stat.Exit(expr)
    }

    override fun visitPrint(ctx: WaccParser.PrintContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        return Stat.Print(expr)
    }

    override fun visitPrintln(ctx: WaccParser.PrintlnContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        return Stat.Println(expr)
    }

    override fun visitIfThenElse(ctx: WaccParser.IfThenElseContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        val stat1 = visit(ctx?.stat(0))
        val stat2 = visit(ctx?.stat(1))
        return Stat.IfThenElse(expr, stat1, stat2)
    }

    override fun visitWhileDo(ctx: WaccParser.WhileDoContext?): Stat {
        val expr = exprVisitor.visit(ctx?.expr())
        val stat = visit(ctx?.stat())
        return Stat.WhileDo(expr, stat)
    }

    override fun visitBegin(ctx: WaccParser.BeginContext?): Stat {
        val stat = visit(ctx?.stat())
        return Stat.Begin(stat)
    }

    override fun visitCompose(ctx: WaccParser.ComposeContext?): Stat {
        val stat1 = visit(ctx?.stat(0))
        val stat2 = visit(ctx?.stat(1))
        return Stat.Compose(stat1, stat2)
    }
}