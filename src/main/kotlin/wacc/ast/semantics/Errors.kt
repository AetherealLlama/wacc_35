package wacc.ast.semantics

import wacc.ast.*

abstract class ProgramError(
        val isSemantic: Boolean,
        private val errName: String,
        private val pos: FilePos
) : Comparable<ProgramError> {
    override fun toString(): String {
        return "${if (isSemantic) "Semantic" else "Syntax"} Error - $errName: $msg (at ${pos.line}:${pos.posInLine})"
    }

    abstract val msg: String

    override fun compareTo(other: ProgramError): Int {
        if (!this.isSemantic && other.isSemantic) return -1
        if (this.isSemantic && !other.isSemantic) return 1
        return this.pos.compareTo(other.pos)
    }
}

// <editor-fold desc="Syntax Errors">

abstract class SyntaxError(errName: String, pos: FilePos) : ProgramError(false, errName, pos)

class FunctionEndError(val name: String, pos: FilePos) : SyntaxError("invalid syntax", pos) {
    override val msg: String
        get() = "any execution path through function `$name` must end with `exit` or `return`"
}

class ProgramEndError(pos: FilePos) : SyntaxError("invalid syntax", pos) {
    override val msg = "any execution path through program must end with `exit`"
}

class CallWrongNumberOfArguments(val given: Int, val needed: Int, val name: String, pos: FilePos) : SyntaxError("call with wrong number of arguments", pos) {
    override val msg: String
        get() = "function `$name` needs $needed arguments but was instead given $given"
}

class IntTooBig(val number: Long, val pos: FilePos) : SyntaxError("numeric value too large", pos) {
    override val msg: String
        get() = "the value $number is too large to be assigned"
}

// </editor-fold>

// <editor-fold desc="Semantic Errors">

abstract class SemanticError(errName: String, pos: FilePos) : ProgramError(true, errName, pos)

class IdentNotFoundError(val name: String, pos: FilePos) : SemanticError("identifier not found", pos) {
    override val msg: String
        get() = "`$name` does not exist"
}

class DuplicateDeclarationError(val name: String, pos: FilePos) : SemanticError("duplicate declaration", pos) {
    override val msg: String
        get() = "`$name` has already been declared"
}

class FunctionRedefinition(val name: String, pos: FilePos) : SemanticError("function redefinition", pos) {
    override val msg: String
        get() = "function `$name` has already been defined"
}

class PairDereferenceNull(pos: FilePos) : SemanticError("null access", pos) {
    override val msg: String = "null cannot be dereferenced as a pair"
}

class ReturnOutsideFuncError(pos: FilePos) : SemanticError("return outside of function", pos) {
    override val msg = "`return` cannot be used when outside a function"
}

// <editor-fold desc="Type Errors">


abstract class TypeError(pos: FilePos) : SemanticError("type mismatch", pos)

class TypeMismatch(val expected: Type, val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "expected `$expected`, but got `$actual`"
}

class ReadTypeMismatch(val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "attempted to read to a `$actual` type"
}

class ReturnTypeMismatch(val f: Func, val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "Attempted to return a `$actual` from `${f.name}()`, which has type `${f.type}`"
}

class ExitTypeMismatch(val actual: Type, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "tried to exit with a `$actual` instead of an integer"
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
        get() = "`$func` received a `$t1`, but needs one of `[${func.argTypes.joinToString(", ")}]`"
}

class UnaryOpInvalidType(val actual: Type, val func: UnaryOperator, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "`$func` needs a `$actual`, but needs a `${func.argType}`"
}

class BinaryArgsMismatch(val t1: Type, val t2: Type, val func: BinaryOperator, pos: FilePos) : TypeError(pos) {
    override val msg: String
        get() = "arguments of `$func` were `$t1` and `$t2`, but they should be the same type"
}

// </editor-fold>

// </editor-fold>
