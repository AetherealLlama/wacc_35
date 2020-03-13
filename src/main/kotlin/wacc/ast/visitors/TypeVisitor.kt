package wacc.ast.visitors

import WaccParserBaseVisitor
import java.lang.IllegalStateException
import wacc.ast.Type

object TypeVisitor : WaccParserBaseVisitor<Type>() {
    override fun visitBaseType(ctx: WaccParser.BaseTypeContext?): Type {
        return when (ctx?.bt?.type) {
            WaccLexer.INT -> Type.BaseType.TypeInt
            WaccLexer.BOOL -> Type.BaseType.TypeBool
            WaccLexer.CHAR -> Type.BaseType.TypeChar
            WaccLexer.STRING -> Type.BaseType.TypeString
            else -> throw IllegalStateException()
        }
    }

    override fun visitArrayType(ctx: WaccParser.ArrayTypeContext?): Type {
        return Type.ArrayType(visit(ctx?.type()))
    }

    override fun visitPairType(ctx: WaccParser.PairTypeContext?): Type {
        val type1 = PairElemTypeVisitor.visit(ctx?.pairElemType(0))
        val type2 = PairElemTypeVisitor.visit(ctx?.pairElemType(1))
        return Type.PairType(type1, type2)
    }

    override fun visitClassType(ctx: WaccParser.ClassTypeContext?): Type {
        return Type.ClassType(ctx!!.IDENT().text)
    }

    object PairElemTypeVisitor : WaccParserBaseVisitor<Type.PairElemType>() {

        override fun visitBasePairElemType(ctx: WaccParser.BasePairElemTypeContext?): Type.PairElemType {
            return when (ctx?.bt?.type) {
                WaccLexer.INT -> Type.BaseType.TypeInt
                WaccLexer.BOOL -> Type.BaseType.TypeBool
                WaccLexer.CHAR -> Type.BaseType.TypeChar
                WaccLexer.STRING -> Type.BaseType.TypeString
                else -> throw IllegalStateException()
            }
        }

        override fun visitArrayPairElemType(ctx: WaccParser.ArrayPairElemTypeContext?): Type.PairElemType =
                Type.ArrayType(TypeVisitor.visit(ctx?.type()))

        override fun visitPairPairElemType(ctx: WaccParser.PairPairElemTypeContext?): Type.PairElemType = Type.PairPairElem
    }
}
