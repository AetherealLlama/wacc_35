package wacc.ast.codegen

import wacc.ast.codegen.types.Function
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Instruction.Special.Label
import wacc.ast.codegen.types.Operand
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Operand.Reg
import wacc.ast.codegen.types.Operation.AddOp
import wacc.ast.codegen.types.Register.*

typealias BuiltinString = Pair<String, String> // Label to Value
typealias BuiltinFunction = Pair<Function, List<BuiltinString>> // The function and string dependencies

val printLnString: BuiltinString = "__s_print_ln" to ""
val printLn: BuiltinFunction = Function(
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
) to listOf(printLnString)

val printIntString: BuiltinString = "__s_print_int" to "%d"
val printInt: BuiltinFunction = Function(
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
) to listOf(printIntString)
