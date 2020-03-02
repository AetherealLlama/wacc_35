package wacc.codegen

import wacc.codegen.types.*
import wacc.codegen.types.Condition.*
import wacc.codegen.types.Function
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Instruction.Special.Label
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operand.Reg
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.Register.*

typealias BuiltinDependency = Pair<List<BuiltinFunction>, List<BuiltinString>>
typealias BuiltinString = Pair<String, String> // Label to Value

data class BuiltinFunction(val function: Function, val deps: BuiltinDependency) {
    val label: Operand.Label
        get() = Operand.Label(this.function.label.name)
}

private val BuiltinString.label: Operand.Label
    get() = Operand.Label(this.first)

// <editor-fold desc="print functions">

val printLnString: BuiltinString = "__s_print_ln" to ""
val printLn: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_ln"),
        listOf(
                Push(listOf(LinkRegister)),
                Load(R0, printLnString.label), // TODO figure out how to get strings back to the data store
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("puts")),
                Move(R0, Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printLnString)))

val printStringString: BuiltinString = "__s_print_string" to "%.*s"
val printString: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_string"),
        listOf(
                Push(listOf(LinkRegister)),
                Load(R1, Reg(R0)),
                Op(AddOp, R2, R0, Imm(4)),
                Load(R0, printStringString.label),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(R0, Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printStringString)))

val printIntString: BuiltinString = "__s_print_int" to "%d"
val printInt: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_int"),
        listOf(
                Push(listOf(LinkRegister)),
                Move(R1, Reg(R0)),
                Load(R0, printIntString.label),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(R0, Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printIntString)))

val printReferenceString: BuiltinString = "__s_print_reference" to "%p"
val printReference: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_reference"),
        listOf(
                Push(listOf(LinkRegister)),
                Move(R1, Reg(R0)),
                Load(R0, printReferenceString.label),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(R0, Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printReferenceString)))

val printBoolTrueString: BuiltinString = "__s_print_bool_true" to "true"
val printBoolFalseString: BuiltinString = "__s_print_bool_false" to "false"
val printBool: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_bool"),
        listOf(
                Push(listOf(LinkRegister)),
                Compare(R0, Imm(0)),
                Load(R0, printBoolTrueString.label, condition = NotEqual),
                Load(R0, printBoolFalseString.label, condition = Equal),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(R0, Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printBoolTrueString, printBoolFalseString)))

// </editor-fold>

// <editor-fold desc="print functions">

val readIntString: BuiltinString = "__s_read_int" to "%d"
val readInt: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_read_int"),
        listOf(
                Push(listOf(LinkRegister)),
                Move(R1, Reg(R0)),
                Load(R0, readIntString.label),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("scanf")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(readIntString)))

val readCharString: BuiltinString = "__s_read_char" to "%c"
val readChar: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_read_char"),
        listOf(
                Push(listOf(LinkRegister)),
                Move(R1, Reg(R0)),
                Load(R0, readCharString.label),
                Op(AddOp, R0, R0, Imm(4)),
                BranchLink(Operand.Label("scanf")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(readCharString)))

// </editor-fold>

// <editor-fold desc="runtime errors">

val throwRuntimeError: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_throw_runtime_error"),
        listOf(
                BranchLink(printString.label),
                Move(R0, Imm(-1)),
                BranchLink(Operand.Label("exit"))
        )
), Pair(listOf(printString), emptyList()))

val overflowErrorString: BuiltinString = "__s_overflow_error" to "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\\n"
val throwOverflowError: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_throw_overflow_error"),
        listOf(
                Load(R0, overflowErrorString.label),
                BranchLink(throwRuntimeError.label)
        )
), listOf(throwRuntimeError) to listOf(overflowErrorString))

// </editor-fold>

// <editor-fold desc="memory stuff">

val negativeArrayIndexString: BuiltinString = "__s_array_index_negative" to "ArrayIndexOutOfBoundsError: negative index\\n"
val arrayIndexTooLargeString: BuiltinString = "__s_array_index_too_large" to "ArrayIndexOutOfBoundsError: index too large\\n"
val checkArrayBounds: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_check_array_bounds"),
        listOf(
                Push(listOf(LinkRegister)),
                Compare(R0, Imm(0)),
                Load(R0, negativeArrayIndexString.label, condition = SignedLess),
                BranchLink(throwRuntimeError.label, condition = SignedLess),
                Load(R1, Reg(R1)),
                Compare(R0, Reg(R1)),
                Load(R0, arrayIndexTooLargeString.label, condition = CarrySet),
                BranchLink(throwRuntimeError.label, condition = CarrySet),
                Pop(listOf(ProgramCounter))
        )
), listOf(throwRuntimeError) to listOf(negativeArrayIndexString, arrayIndexTooLargeString))

val checkNullPointerString: BuiltinString = "__s_check_null_pointer" to "NullReferenceError: dereference a null reference\\n"
val checkNullPointer: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_check_null_pointer"),
        listOf(
                Push(listOf(LinkRegister)),
                Compare(R0, Imm(0)),
                Load(R0, checkNullPointerString.label, condition = Equal),
                BranchLink(throwRuntimeError.label, condition = Equal),
                Pop(listOf(ProgramCounter))
        )
), listOf(throwRuntimeError) to listOf(checkNullPointerString))

val nullPointerDereferenceString: BuiltinString = "__s_null_pointer_deref" to "NullReferenceError: dereference a null reference\\n"
val freePair: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_free_pair"),
        listOf(
                Push(listOf(LinkRegister)),
                Compare(R0, Imm(0)),
                Load(R0, nullPointerDereferenceString.label, condition = Equal),
                Branch(throwRuntimeError.label, condition = Equal),
                Push(listOf(R0)),
                Load(R0, Reg(R0)),
                BranchLink(Operand.Label("free")),
                Load(R0, Reg(StackPointer)),
                Load(R0, Reg(StackPointer), Imm(4)),
                BranchLink(Operand.Label("free")),
                Pop(listOf(R0)),
                BranchLink(Operand.Label("free")),
                Pop(listOf(ProgramCounter))
        )
), listOf(throwRuntimeError) to listOf(nullPointerDereferenceString))

// </editor-fold>
