package wacc.ast.semantics

import wacc.ast.BinaryOperator
import wacc.ast.FilePos
import wacc.ast.Type
import wacc.ast.UnaryOperator

abstract class SemanticError(private val errName: String? = null, private val pos: FilePos) {
    override fun toString(): String {
        return "Error - $errName: $msg (at ${pos.line}:${pos.posInLine})"
    }

    abstract val msg: String
}

class FunctionEndError(val name: String, pos: FilePos) : SemanticError("invalid syntax", pos) {
    override val msg: String
        get() = "function `$name` must end with `exit` or `return`"
}

class IdentNotFoundError(val name: String, pos: FilePos) : SemanticError("identifier not found", pos) {
    override val msg: String
        get() = "`$name` does not exist"
}

class DuplicateDeclarationError(val name: String, pos: FilePos) : SemanticError("duplicate declaration", pos) {
    override val msg: String
        get() = "`$name` has already been declared"
}


abstract class TypeError(pos: FilePos) : SemanticError("type mismatch", pos)

class TypeMismatch(val expected: Type, val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "expected `$expected`, but got `$actual`"
}

class ReadTypeMismatch(val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "attempted to read to a `$actual` type"
}

class InvalidPairElemType(val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "values of type `$actual` cannot be put in a pair"
}

class FreeTypeMismatch(val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "cannot free a variable of type `$actual`"
}

class BinaryOpInvalidType(val t1: Type, val func: BinaryOperator, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "`$func` needs a `$t1`, but needs one of `[${func.argTypes.joinToString(", ")}]`"
}

class UnaryOpInvalidType(val actual: Type, val func: UnaryOperator, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "`$func` needs a `$actual`, but needs a `${func.argType}`"
}

class BinaryArgsMismatch(val t1: Type, val t2: Type, val func: BinaryOperator, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "arguments of `$func` were `$t1` and `$t2`, but they should be the same type"
}

