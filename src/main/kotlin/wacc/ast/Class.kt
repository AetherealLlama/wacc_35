package wacc.ast

class Class(pos: FilePos, val name: String, val fields: List<Param>, val funcs: List<Func>) : ASTNode(pos)