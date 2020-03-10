package wacc.ast

import wacc.checker.asNormalType

/**
 * The type of an AssignLhs, AssignRhs or an expression
 */
sealed class Type {
    abstract val display: String
    override fun toString(): String = display

    sealed class BaseType : Type(), PairElemType {
        object TypeInt : BaseType() { override val display = "Int" }
        object TypeBool : BaseType() { override val display = "Bool" }
        object TypeChar : BaseType() { override val display = "Char" }
        object TypeString : BaseType() { override val display = "String" }
    }
    data class ArrayType(val type: Type) : Type(), PairElemType {
        override val display: String
            get() = "${type.display}[]"
    }
    data class PairType(val type1: PairElemType, val type2: PairElemType) : Type() {
        override val display: String
            get() = "(${type1.asNormalType.display}, ${type1.asNormalType.display})"
    }
    data class ClassType(val className: String): Type() {
        override val display: String
            get() = className
    }

    // HACK: is there any better way of doing this; we can't guarantee someone implementing this and using it
    interface PairElemType

    object PairPairElem : PairElemType

    // This is only used to be able to reason about empty arrays during semantics checking.
    object AnyType : Type(), PairElemType { override val display = "T" }
}
