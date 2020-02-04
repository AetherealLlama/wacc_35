package wacc.ast.semantics

import wacc.ast.Func
import wacc.ast.Program
import wacc.ast.Stat
import wacc.ast.Type


typealias Scope = List<Pair<String, Type>>


fun Program.checkSemantics() {
    funcs.forEach { f -> f.checkSemantics(funcs) }
    stat.checkSemantics(funcs)
}

fun Func.checkSemantics(funcs: Array<Func>) {
    stat.checkSemantics(funcs, currentScope = params.map { it.name to it.type })
}

fun Stat.checkSemantics(
        funcs: Array<Func>,
        scopes: List<Scope> = listOf(emptyList()),
        currentScope: Scope = emptyList()
) {
    
}