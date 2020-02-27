package wacc.ast.codegen

import wacc.ast.codegen.types.Condition
import wacc.ast.codegen.types.Function
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Instruction.Special.Label
import wacc.ast.codegen.types.Operand
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Operand.Reg
import wacc.ast.codegen.types.Operation.AddOp
import wacc.ast.codegen.types.Register.*

typealias BuiltinDependency = Pair<List<BuiltinFunction>, List<BuiltinString>>
typealias BuiltinString = Pair<String, String> // Label to Value

data class BuiltinFunction(val function: Function, val deps: BuiltinDependency)

// <editor-fold desc="print functions">

val printLnString: BuiltinString = "__s_print_ln" to ""
val printLn: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_ln"),
        listOf(
                Push(listOf(LinkRegister)),
                Load(GeneralRegister(0), Operand.Label(printLnString.first)), // TODO figure out how to get strings back to the data store
                Op(AddOp, GeneralRegister(0), GeneralRegister(0), Imm(4)),
                BranchLink(Operand.Label("puts")),
                Move(GeneralRegister(0), Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printLnString)))

val printStringString: BuiltinString = "__s_print_string" to "%.*s"
val printString: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_string"),
        listOf(
                Push(listOf(LinkRegister)),
                Load(GeneralRegister(1), Reg(GeneralRegister(0))),
                Op(AddOp, GeneralRegister(2), GeneralRegister(0), Imm(4)),
                Load(GeneralRegister(0), Operand.Label(printStringString.first)),
                Op(AddOp, GeneralRegister(0), GeneralRegister(0), Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(GeneralRegister(0), Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printStringString)))

val printIntString: BuiltinString = "__s_print_int" to "%d"
val printInt: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_print_int"),
        listOf(
                Push(listOf(LinkRegister)),
                Move(GeneralRegister(1), Reg(GeneralRegister(0))),
                Load(GeneralRegister(0), Operand.Label(printIntString.first)),
                Op(AddOp, GeneralRegister(0), GeneralRegister(0), Imm(4)),
                BranchLink(Operand.Label("printf")),
                Move(GeneralRegister(0), Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
), Pair(emptyList(), listOf(printIntString)))

// </editor-fold>

// <editor-fold desc="runtime errors">

val throwRuntimeError: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_throw_runtime_error"),
        listOf(
                BranchLink(Operand.Label(printString.function.label.name)),
                Move(GeneralRegister(0), Imm(-1)),
                BranchLink(Operand.Label("exit"))
        )
), Pair(listOf(printString), emptyList()))

val overflowErrorString: BuiltinString = "__s_overflow_error" to "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\n"
val throwOverflowError: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_throw_overflow_error"),
        listOf(
                Load(GeneralRegister(0), Operand.Label(overflowErrorString.first)),
                BranchLink(Operand.Label(throwRuntimeError.function.label.name))
        )
), listOf(throwRuntimeError) to listOf(overflowErrorString))

// </editor-fold>

// <editor-fold desc="array checks">

val negativeArrayIndexString: BuiltinString = "__s_array_index_negative" to "ArrayIndexOutOfBoundsError: negative index\n"
val arrayIndexTooLargeString: BuiltinString = "__s_array_index_too_large" to "ArrayIndexOutOfBoundsError: index too large\n"
val checkArrayBounds: BuiltinFunction = BuiltinFunction(Function(
        Label("__f_check_array_bounds"),
        listOf(
                Push(listOf(LinkRegister)),
                Compare(GeneralRegister(0), Imm(0)),
                Load(GeneralRegister(0), Operand.Label(negativeArrayIndexString.first), condition = Condition.SignedLess),
                BranchLink(Operand.Label(throwRuntimeError.function.label.name), condition = Condition.SignedLess),
                Load(GeneralRegister(1), Reg(GeneralRegister(1))),
                Compare(GeneralRegister(0), Reg(GeneralRegister(1))),
                Load(GeneralRegister(0), Operand.Label(arrayIndexTooLargeString.first), condition = Condition.CarrySet),
                BranchLink(Operand.Label(throwRuntimeError.function.label.name), condition = Condition.CarrySet),
                Pop(listOf(ProgramCounter))
        )
), listOf(throwRuntimeError) to listOf(negativeArrayIndexString, arrayIndexTooLargeString))

// </editor-fold>
