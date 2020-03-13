package wacc.checker

import wacc.ast.BinaryOperator
import wacc.ast.FilePos
import wacc.ast.Type
import wacc.ast.Type.BaseType.*
import wacc.ast.UnaryOperator

internal val ANY_PAIR = Type.PairType(Type.AnyType, Type.AnyType)

internal infix fun Type.matches(other: Type): Boolean {
    if (this is Type.AnyType || other is Type.AnyType) return true
    if (this is Type.ArrayType && other is Type.ArrayType) return this.type matches other.type
    if (this is Type.PairType && other is Type.PairType) {
        return (this.type1.asNormalType matches other.type1.asNormalType) &&
                (this.type2.asNormalType matches other.type2.asNormalType)
    }
    if (this is Type.ClassType && other is Type.ClassType) return this.className == other.className
    return this.javaClass == other.javaClass
}

internal val Type.PairElemType.asNormalType: Type
    get() = if (this is Type.PairPairElem) ANY_PAIR else this as Type

internal val Type.asPairElemType: Type.PairElemType?
    get() {
        if (this is Type.PairType)
            return Type.PairPairElem
        if (this is Type.PairElemType)
            return this
        return null
    }

internal fun Type.checkArrayType(depth: Int, pos: FilePos): Pair<Type, Errors> {
    if (depth == 0)
        return this to emptyList()

    if (this is Type.ArrayType)
        return this.type.checkArrayType(depth - 1, pos)

    var expectedType: Type = Type.AnyType
    repeat(depth) { expectedType = Type.ArrayType(expectedType) }
    return Type.AnyType to listOf(TypeMismatch(expectedType, this, pos))
}

// <editor-fold desc="Operator Types">

private val unaryOpTypes: Map<UnaryOperator, Pair<Type, Type>> = mapOf(
        UnaryOperator.BANG to (TypeBool to TypeBool),
        UnaryOperator.MINUS to (TypeInt to TypeInt),
        UnaryOperator.LEN to (Type.ArrayType(Type.AnyType) to TypeInt),
        UnaryOperator.ORD to (TypeChar to TypeInt),
        UnaryOperator.CHR to (TypeInt to TypeChar),
        UnaryOperator.BNOT to (TypeInt to TypeInt)
)

internal val UnaryOperator.argType: Type
    get() = unaryOpTypes.getValue(this).first

internal val UnaryOperator.returnType: Type
    get() = unaryOpTypes.getValue(this).second

internal val BinaryOperator.argTypes: List<Type>
    get() = when (this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB,
        BinaryOperator.BAND,
        BinaryOperator.BOR,
        BinaryOperator.BXOR,
        BinaryOperator.BLEFT,
        BinaryOperator.BRIGHT -> listOf(TypeInt)
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE -> listOf(TypeInt, TypeChar)
        BinaryOperator.EQ,
        BinaryOperator.NEQ -> listOf(TypeInt, TypeBool, TypeChar, TypeString,
                Type.ArrayType(Type.AnyType), Type.PairType(Type.AnyType, Type.AnyType))
        BinaryOperator.LAND,
        BinaryOperator.LOR -> listOf(TypeBool)
    }

internal val BinaryOperator.returnType: Type
    get() = when (this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB,
        BinaryOperator.BAND,
        BinaryOperator.BOR,
        BinaryOperator.BXOR,
        BinaryOperator.BLEFT,
        BinaryOperator.BRIGHT -> TypeInt
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE,
        BinaryOperator.EQ,
        BinaryOperator.NEQ,
        BinaryOperator.LAND,
        BinaryOperator.LOR -> TypeBool
    }

// </editor-fold>
