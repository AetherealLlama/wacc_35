package wacc.utils

import org.koin.dsl.module
import wacc.ast.visitors.*

val waccModule = module {
    single { ProgramVisitor() }
    single { ClassVisitor() }
    single { IncludeVisitor() }
    single { FunctionVisitor() }
    single { StatVisitor() }
    single { ExprVisitor() }
    single { TypeVisitor() }
    single { AssignRhsVisitor() }
    single { AssignLhsVisitor() }
}
