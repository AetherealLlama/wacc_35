package wacc.ast

typealias Field = Param

class Class(pos: FilePos, val name: String, val fields: List<Field>, val funcs: List<Func>) : ASTNode(pos)
