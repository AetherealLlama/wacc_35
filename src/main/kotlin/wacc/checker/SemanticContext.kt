package wacc.checker

import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Class

class SemanticContext(
    val program: Program,
    val func: Func?,
    val cls: Class?,
    val isLastStat: Boolean,
    val scopes: List<Scope> = emptyList()
) {
    fun withLastStat(isLastStat: Boolean): SemanticContext =
            SemanticContext(program, func, cls, isLastStat, scopes)

    fun withModifiedScope(currentScope: Scope) =
            SemanticContext(program, func, cls, isLastStat, listOf(currentScope) + scopes.drop(1))

    fun withNewScope(newScope: Scope = emptyList()): SemanticContext =
            SemanticContext(program, func, cls, isLastStat, listOf(newScope) + scopes)

    val currentScope: Scope
        get() = scopes[0]
}
