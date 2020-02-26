package wacc.ast.codegen

import wacc.ast.codegen.types.Function
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Instruction.Special.Label
import wacc.ast.codegen.types.Operand
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Operation.AddOp
import wacc.ast.codegen.types.Register.*

val printLnString = "\\0"
val printLn = Function(
        Label("__f_print_ln"),
        listOf(
                Push(listOf(LinkRegister)),
                Load(GeneralRegister(0), Operand.Label("__s_print_ln")), // TODO figure out how to get strings back to the data store
                Op(AddOp, GeneralRegister(0), GeneralRegister(0), Imm(4)),
                BranchLink(Operand.Label("puts")),
                Move(GeneralRegister(0), Imm(0)),
                BranchLink(Operand.Label("fflush")),
                Pop(listOf(ProgramCounter))
        )
)
