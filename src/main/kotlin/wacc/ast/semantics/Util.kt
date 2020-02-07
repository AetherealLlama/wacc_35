package wacc.ast.semantics

import wacc.ast.BinaryOperator
import wacc.ast.FilePos
import wacc.ast.Type
import wacc.ast.UnaryOperator


internal val ANY_PAIR = Type.PairType(Type.AnyType, Type.AnyType)


internal infix fun Type.matches(other: Type): Boolean {
    if (this is Type.AnyType || other is Type.AnyType) return true
    if (this is Type.ArrayType && other is Type.ArrayType) return this.type matches other.type
    if (this is Type.PairType && other is Type.PairType) {
        return (this.type1.normalType matches other.type1.normalType)
                && (this.type2.normalType matches other.type2.normalType)
    }
    return this.javaClass == other.javaClass
}

internal val Type.PairElemType.normalType: Type
    get() = if (this is Type.PairPairElem) ANY_PAIR else this as Type

internal fun Type.checkArrayType(depth: Int, pos: FilePos): Pair<Type, Errors> {
    if (depth == 0)
        return this to emptyList()

    if (this is Type.ArrayType)
        return this.type.checkArrayType(depth-1, pos)

    var expectedType: Type = Type.AnyType
    repeat(depth) { expectedType = Type.ArrayType(expectedType) }
    return Type.AnyType to listOf(TypeMismatch(expectedType, this, pos))
}

// <editor-fold desc="Operator Types">

private val unaryOpTypes: Map<UnaryOperator, Pair<Type, Type>> = mapOf(
        UnaryOperator.BANG  to (Type.BaseType.TypeBool to Type.BaseType.TypeBool),
        UnaryOperator.MINUS to (Type.BaseType.TypeInt to Type.BaseType.TypeInt),
        UnaryOperator.LEN   to (Type.ArrayType(Type.AnyType) to Type.BaseType.TypeInt),
        UnaryOperator.ORD   to (Type.BaseType.TypeChar to Type.BaseType.TypeInt),
        UnaryOperator.CHR   to (Type.BaseType.TypeInt  to Type.BaseType.TypeChar)
)

internal val UnaryOperator.argType: Type
    get() = unaryOpTypes.getValue(this).first

internal val UnaryOperator.returnType: Type
    get() = unaryOpTypes.getValue(this).second

internal val BinaryOperator.argTypes: List<Type>
    get() = when(this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB -> listOf(Type.BaseType.TypeInt)
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE -> listOf(Type.BaseType.TypeInt, Type.BaseType.TypeChar)
        BinaryOperator.EQ,
        BinaryOperator.NEQ -> listOf(Type.BaseType.TypeInt, Type.BaseType.TypeBool, Type.BaseType.TypeChar,
                Type.ArrayType(Type.AnyType), Type.PairType(Type.AnyType, Type.AnyType))
        BinaryOperator.LAND,
        BinaryOperator.LOR -> listOf(Type.BaseType.TypeBool)
    }

internal val BinaryOperator.returnType: Type
    get() = when(this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB -> Type.BaseType.TypeInt
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE,
        BinaryOperator.EQ,
        BinaryOperator.NEQ,
        BinaryOperator.LAND,
        BinaryOperator.LOR -> Type.BaseType.TypeBool
    }

// </editor-fold>
