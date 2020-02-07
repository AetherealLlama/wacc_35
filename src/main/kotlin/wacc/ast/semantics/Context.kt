package wacc.ast.semantics

import wacc.ast.Func

class SemanticContext(
        val funcs: Array<Func>,
        val func: Func?,
        val isLastStat: Boolean,
        val scopes: List<Scope> = emptyList()
) {
    fun withLastStat(isLastStat: Boolean): SemanticContext =
            SemanticContext(funcs, func, isLastStat, scopes)

    fun withModifiedScope(currentScope: Scope) =
            SemanticContext(funcs, func, isLastStat, listOf(currentScope) + scopes.drop(1))

    fun withNewScope(newScope: Scope = emptyList()): SemanticContext =
            SemanticContext(funcs, func, isLastStat, listOf(newScope) + scopes)

    val currentScope: Scope
        get() = scopes[0]
}