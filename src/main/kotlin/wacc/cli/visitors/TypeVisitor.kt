package wacc.cli.visitors

import WaccParserBaseVisitor
import wacc.ast.Type

class TypeVisitor : WaccParserBaseVisitor<Type>() {
    private val pairElemTypeVisitor = PairElemTypeVisitor()

    override fun visitBaseType(ctx: WaccParser.BaseTypeContext?): Type {
        return when (ctx?.BASETYPE()?.symbol?.type) {
            WaccLexer.INT -> Type.BaseType.TypeInt
            WaccLexer.BOOL -> Type.BaseType.TypeBool
            WaccLexer.CHAR -> Type.BaseType.TypeChar
            WaccLexer.STRING -> Type.BaseType.TypeString
            // TODO: find a better alternative than returning TypeInt
            else -> Type.BaseType.TypeInt
        }
    }

    override fun visitArrayType(ctx: WaccParser.ArrayTypeContext?): Type {
        return Type.ArrayType(visit(ctx?.type()))
    }

    override fun visitPairType(ctx: WaccParser.PairTypeContext?): Type {
        val type1 = pairElemTypeVisitor.visit(ctx?.pairElemType(0))
        val type2 = pairElemTypeVisitor.visit(ctx?.pairElemType(1))
        return Type.PairType(type1, type2)
    }

    inner class PairElemTypeVisitor : WaccParserBaseVisitor<Type.PairElemType>() {
        // Bit of an ugly hack to avoid recursive dependencies
        private val typeVisitor: TypeVisitor = this@TypeVisitor

        override fun visitBasePairElemType(ctx: WaccParser.BasePairElemTypeContext?): Type.PairElemType {
            // TODO: find a way to remove code duplication here
            return when (ctx?.BASETYPE()?.symbol?.type) {
                WaccLexer.INT -> Type.BaseType.TypeInt
                WaccLexer.BOOL -> Type.BaseType.TypeBool
                WaccLexer.CHAR -> Type.BaseType.TypeChar
                WaccLexer.STRING -> Type.BaseType.TypeString
                else -> Type.BaseType.TypeInt
            }
        }

        override fun visitArrayPairElemType(ctx: WaccParser.ArrayPairElemTypeContext?): Type.PairElemType =
                Type.ArrayType(typeVisitor.visit(ctx?.type()))

        override fun visitPairPairElemType(ctx: WaccParser.PairPairElemTypeContext?): Type.PairElemType = Type.PairPairElem
    }
}
