package wacc.ast.semantics.errors

import wacc.ast.BinaryOperator
import wacc.ast.Type
import wacc.ast.UnaryOperator
import wacc.ast.semantics.argType
import wacc.ast.semantics.argTypes
import wacc.ast.semantics.returnType

abstract class SemanticError(private val errName: String? = null) {
    override fun toString(): String {
        return "Error - $errName: "
    }

    abstract val msg: String
}

data class FunctionEndError(val name: String) : SemanticError("invalid syntax") {
    override val msg: String
        get() = "function `$name` must end with `exit` or `return`"
}

data class IdentNotFoundError(val name: String) : SemanticError("identifier not found") {
    override val msg: String
        get() = "`$name` does not exist"
}

data class DuplicateDeclarationError(val name: String) : SemanticError("duplicate declaration") {
    override val msg: String
        get() = "`$name` has already been declared"
}


abstract class TypeError : SemanticError("type mismatch")

data class TypeMismatch(val expected: Type, val actual: Type) : TypeError() {
    override val msg: String
        get() = "expected `$expected`, but got `$actual`"
}

data class ReadTypeMismatch(val actual: Type) : TypeError() {
    override val msg: String
        get() = "attempted to read to a `$actual` type"
}

data class InvalidPairElemType(val actual: Type) : TypeError() {
    override val msg: String
        get() = "values of type `$actual` cannot be put in a pair"
}

data class FreeTypeMismatch(val actual: Type) : TypeError() {
    override val msg: String
        get() = "cannot free a variable of type `$actual`"
}

data class BinaryOpInvalidType(val t1: Type, val func: BinaryOperator) : TypeError() {
    override val msg: String
        get() = "`$func` needs a `$t1`, but needs one of `[${func.argTypes.joinToString(", ")}]`"
}

data class UnaryOpInvalidType(val actual: Type, val func: UnaryOperator) : TypeError() {
    override val msg: String
        get() = "`$func` needs a `$actual`, but needs a `${func.argType}`"
}

data class BinaryArgsMismatch(val t1: Type, val t2: Type, val func: BinaryOperator) : TypeError() {
    override val msg: String
        get() = "arguements of `$func` were `$t1` and `$t2`, but they should be the same type"
}

