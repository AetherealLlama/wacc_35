package wacc.ast

sealed class Type {
    sealed class BaseType : Type(), PairElemType  {
        object TypeInt : BaseType()
        object TypeBool : BaseType()
        object TypeChar : BaseType()
        object TypeString : BaseType()
    }

    data class ArrayType(val type: Type) : Type(), PairElemType
    data class PairType(val type1: PairElemType, val type2: PairElemType)

    interface PairElemType
    object PairPairElem : PairElemType
}
