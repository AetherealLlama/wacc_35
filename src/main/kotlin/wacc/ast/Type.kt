package wacc.ast

import WaccParser.*

sealed class Type {
    sealed class BaseType : Type(), PairElemType {
        object TypeInt : BaseType()
        object TypeBool : BaseType()
        object TypeChar : BaseType()
        object TypeString : BaseType()
    }

    data class ArrayType(val type: Type) : Type(), PairElemType
    data class PairType(val type1: PairElemType, val type2: PairElemType) : Type()

    // HACK: is there any better way of doing this; we can't guarantee someone implementing this and using it
    interface PairElemType

    object PairPairElem : PairElemType
}

fun getTypeFromContext(ctx: TypeContext): Type {
    when (ctx) {
        is BaseTypeContext -> when (ctx.BASETYPE().symbol.type) {
            WaccLexer.INT -> return Type.BaseType.TypeInt
            WaccLexer.BOOL -> return Type.BaseType.TypeBool
            WaccLexer.CHAR -> return Type.BaseType.TypeChar
            WaccLexer.STRING -> return Type.BaseType.TypeString
        }
        is ArrayTypeContext ->
            return Type.ArrayType(getTypeFromContext(ctx.type()))
        is PairTypeContext -> {
            val ctx1 = ctx.pairElemType(0)
            val ctx2 = ctx.pairElemType(1)
            val elem1 = getPairElemTypeFromContext(ctx1)
            val elem2 = getPairElemTypeFromContext(ctx2)
            return Type.PairType(elem1, elem2)
        }
    }
    // TODO: replace this with something else as an error value
    return Type.BaseType.TypeInt
}

fun getPairElemTypeFromContext(ctx: PairElemTypeContext): Type.PairElemType {
    var pairElemType: Type.PairElemType = Type.PairPairElem
    when (ctx) {
        is BasePairElemTypeContext -> when (ctx.BASETYPE().symbol.type) {
            WaccLexer.INT -> pairElemType = Type.BaseType.TypeInt
            WaccLexer.BOOL -> pairElemType = Type.BaseType.TypeBool
            WaccLexer.CHAR -> pairElemType = Type.BaseType.TypeChar
            WaccLexer.STRING -> pairElemType = Type.BaseType.TypeString
        }
        is ArrayPairElemTypeContext ->
            pairElemType = Type.ArrayType(getTypeFromContext(ctx.type()))
        is PairPairElemTypeContext ->
            pairElemType = Type.PairPairElem
    }
    return pairElemType
}