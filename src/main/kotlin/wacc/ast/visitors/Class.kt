package wacc.ast.visitors

import WaccParserBaseVisitor
import wacc.ast.Class

class ClassVisitor : WaccParserBaseVisitor<Class>() {
    override fun visitClazz(ctx: WaccParser.ClazzContext?): Class {
        return TODO()
    }
}