package wacc.cli.visitors

import WaccParserBaseVisitor
import org.antlr.v4.runtime.Vocabulary
import wacc.ast.Type
import java.lang.IllegalStateException

internal fun Vocabulary.getOriginalName(symbol: Int) = getLiteralName(symbol).replace("'", "")

class TypeVisitor : WaccParserBaseVisitor<Type>() {
    private val pairElemTypeVisitor = PairElemTypeVisitor()
    private val vocabulary = WaccLexer.VOCABULARY

    override fun visitBaseType(ctx: WaccParser.BaseTypeContext?): Type {
        return when (ctx?.BASETYPE()?.text) {
            vocabulary.getOriginalName(WaccLexer.INT) -> Type.BaseType.TypeInt
            vocabulary.getOriginalName(WaccLexer.BOOL) -> Type.BaseType.TypeBool
            vocabulary.getOriginalName(WaccLexer.CHAR) -> Type.BaseType.TypeChar
            vocabulary.getOriginalName(WaccLexer.STRING) -> Type.BaseType.TypeString
            else -> throw IllegalStateException()
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
            return when (ctx?.BASETYPE()?.text) {
                vocabulary.getOriginalName(WaccLexer.INT) -> Type.BaseType.TypeInt
                vocabulary.getOriginalName(WaccLexer.BOOL) -> Type.BaseType.TypeBool
                vocabulary.getOriginalName(WaccLexer.CHAR) -> Type.BaseType.TypeChar
                vocabulary.getOriginalName(WaccLexer.STRING) -> Type.BaseType.TypeString
                else -> throw IllegalStateException()
            }
        }

        override fun visitArrayPairElemType(ctx: WaccParser.ArrayPairElemTypeContext?): Type.PairElemType =
                Type.ArrayType(typeVisitor.visit(ctx?.type()))

        override fun visitPairPairElemType(ctx: WaccParser.PairPairElemTypeContext?): Type.PairElemType = Type.PairPairElem
    }
}
