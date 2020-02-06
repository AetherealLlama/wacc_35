package wacc.ast.semantics

import wacc.ast.*


internal typealias Scope = List<Pair<String, Type>>
internal typealias Errors = List<SemanticError>


fun Program.checkSemantics(): Errors =
    funcs.flatMap { it.checkSemantics(funcs) } + stat.checkSemantics(funcs).second

private fun Func.checkSemantics(funcs: Array<Func>): Errors {
    var lastStatementError = emptyList<SemanticError>()
    var lastStat = stat
    while (lastStat is Stat.Compose)
        lastStat = lastStat.stat2
    if (lastStat !is Stat.Exit && lastStat !is Stat.Compose)
        lastStatementError = listOf(FunctionEndError(name, pos))
    return stat.checkSemantics(funcs, currentScope = params.map { it.name to it.type }).second + lastStatementError
}

private fun Stat.checkSemantics(
        funcs: Array<Func>,
        scopes: List<Scope> = listOf(emptyList()),
        currentScope: Scope = emptyList()
): Pair<Scope, Errors> = when(this) {
    is Stat.Skip -> currentScope to emptyList()
    is Stat.AssignNew -> {
        val (rhsType, rhsErrors) = rhs.checkSemantics(funcs, listOf(currentScope) + scopes)
        var typeError = emptyList<SemanticError>()
        if (!(rhsType matches type))
            typeError = listOf(TypeMismatch(type, rhsType, pos))
        if (name in scopes.flatten().map { it.first })
            currentScope to rhsErrors + typeError + listOf(DuplicateDeclarationError(name, pos))
        else
            (currentScope + (name to type)) to rhsErrors + typeError
    }
    is Stat.Assign -> {
        val (lhsType, lhsErrors) = lhs.checkSemantics(funcs, listOf(currentScope) + scopes)
        val (rhsType, rhsErrors) = rhs.checkSemantics(funcs, listOf(currentScope) + scopes)
        var typeError = emptyList<SemanticError>()
        if (!(lhsType matches rhsType))
            typeError = listOf(TypeMismatch(lhsType, rhsType, pos))
        currentScope to lhsErrors + rhsErrors + typeError
    }
    is Stat.Read -> lhs.checkSemantics(funcs, listOf(currentScope) + scopes).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.BaseType.TypeInt, Type.BaseType.TypeChar).none { type matches it })
            typeError = listOf(ReadTypeMismatch(type, pos))
        currentScope to errors + typeError
    }
    is Stat.Free -> expr.checkSemantics(funcs, scopes).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.ArrayType(Type.AnyType), ANY_PAIR).none { type matches it })
            typeError = listOf(FreeTypeMismatch(type, pos))
        currentScope to errors + typeError
    }
    is Stat.Return,
    is Stat.Exit,
    is Stat.Print,
    is Stat.Println -> currentScope to emptyList()
    is Stat.IfThenElse -> {
        val exprErrors = expr.checkBool(funcs, listOf(currentScope) + scopes)
        val (_, branch1Errors) = branch1.checkSemantics(funcs, listOf(currentScope) + scopes)
        val (_, branch2Errors) = branch2.checkSemantics(funcs, listOf(currentScope) + scopes)
        currentScope to exprErrors + branch1Errors + branch2Errors
    }
    is Stat.WhileDo -> {
        val exprErrors = expr.checkBool(funcs, listOf(currentScope) + scopes)
        val (_, statErrors) = stat.checkSemantics(funcs, listOf(currentScope) + scopes)
        currentScope to exprErrors + statErrors
    }
    is Stat.Begin -> currentScope to stat.checkSemantics(funcs, listOf(currentScope) + scopes).second
    is Stat.Compose -> {
        val (scope1, stat1Errors) = stat1.checkSemantics(funcs, scopes, currentScope)
        val (scope2, stat2Errors) = stat2.checkSemantics(funcs, scopes, scope1)
        scope2 to (stat1Errors + stat2Errors)
    }
}

private fun Expr.checkSemantics(funcs: Array<Func>, scopes: List<Scope>): Pair<Type, Errors> = when(this) {
    is Expr.Literal.IntLiteral -> Type.BaseType.TypeInt to emptyList()
    is Expr.Literal.BoolLiteral -> Type.BaseType.TypeBool to emptyList()
    is Expr.Literal.CharLiteral -> Type.BaseType.TypeChar to emptyList()
    is Expr.Literal.StringLiteral -> Type.BaseType.TypeString to emptyList()
    is Expr.Literal.PairLiteral -> ANY_PAIR to emptyList()
    is Expr.Ident -> checkIdent(name, scopes, pos)
    is Expr.ArrayElem -> checkArrayElem(name, exprs, funcs, scopes, pos)
    is Expr.UnaryOp -> expr.checkSemantics(funcs, scopes).let { (type, errors) ->
        var argTypeError = emptyList<SemanticError>()
        if (!(type matches operator.argType)) argTypeError = listOf(UnaryOpInvalidType(type, operator, pos))
        operator.returnType to errors + argTypeError
    }
    is Expr.BinaryOp -> {
        val (arg1Type, arg1Errors) = expr1.checkSemantics(funcs, scopes)
        val (arg2Type, arg2Errors) = expr2.checkSemantics(funcs, scopes)
        val errors = mutableListOf<SemanticError>().apply { addAll(arg1Errors); addAll(arg2Errors) }
        // Args must match possible function's accepted types
        if (operator.argTypes.none { arg1Type matches it }) errors += BinaryOpInvalidType(arg1Type, operator, pos)
        // Args must match eachother
        if (!(arg1Type matches arg2Type)) errors += BinaryArgsMismatch(arg1Type, arg2Type, operator, pos)
        operator.returnType to errors
    }
}

private fun AssignLhs.checkSemantics(funcs: Array<Func>, scopes: List<Scope>): Pair<Type, Errors> = when(this) {
    is AssignLhs.Variable -> checkIdent(name, scopes, pos)
    is AssignLhs.ArrayElem -> checkArrayElem(Expr.Ident(pos, name), exprs, funcs, scopes, pos)
    is AssignLhs.PairElem -> expr.checkPairElem(funcs, scopes)(accessor)
}

private fun AssignRhs.checkSemantics(funcs: Array<Func>, scopes: List<Scope>): Pair<Type, Errors> = when(this) {
    is AssignRhs.Expression -> expr.checkSemantics(funcs, scopes)
    is AssignRhs.ArrayLiteral -> {
        val checkedExprs = exprs.map { it.checkSemantics(funcs, scopes) }
        val errors = checkedExprs.flatMap { it.second }
        val type = Type.ArrayType(checkedExprs.firstOrNull()?.first ?: Type.AnyType)
        type to errors
    }
    is AssignRhs.Newpair -> {
        val (fstRawType, fstErrors) = expr1.checkSemantics(funcs, scopes)
        val (sndRawType, sndErrors) = expr2.checkSemantics(funcs, scopes)
        // Make sure elems are types that can be put in pairs
        val (fstType, fstTypeError) =
                if (fstRawType is Type.PairElemType)
                    (fstRawType as Type.PairElemType) to emptyList<SemanticError>()
                else
                    Type.AnyType to listOf(InvalidPairElemType(fstRawType, pos))
        val (sndType, sndTypeError) =
                if (sndRawType is Type.PairElemType)
                    (sndRawType as Type.PairElemType) to emptyList<SemanticError>()
                else
                    Type.AnyType to listOf(InvalidPairElemType(sndRawType, pos))
        Type.PairType(fstType, sndType) to listOf(fstErrors, sndErrors, fstTypeError, sndTypeError).flatten()
    }
    is AssignRhs.PairElem -> expr.checkPairElem(funcs, scopes)(accessor)
    is AssignRhs.Call -> funcs.find { it.name == name }?.let { it.type to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name, pos))
}

// <editor-fold desc="Common type checkers, to keep things dry">

private fun Expr.checkBool(funcs: Array<Func>, scopes: List<Scope>): Errors {
    val (exprType, exprErrors) = checkSemantics(funcs, scopes)
    return if (exprType.matches(Type.BaseType.TypeBool))
        exprErrors
    else
        exprErrors + TypeMismatch(Type.BaseType.TypeBool, exprType, pos)
}

private fun Expr.checkPairElem(funcs: Array<Func>, scopes: List<Scope>): (PairAccessor) -> Pair<Type, Errors> {
    val (exprType, exprErrors) = checkSemantics(funcs, scopes)
    return if (exprType matches ANY_PAIR)
        { accessor -> when (accessor) {
            PairAccessor.FST -> (exprType as Type.PairType).type1
            PairAccessor.SND -> (exprType as Type.PairType).type2
        }.let { type -> type as Type to exprErrors } }
    else
        { _ -> Type.AnyType to exprErrors + TypeMismatch(ANY_PAIR, exprType, pos) }
}

private fun checkArrayElem(
        name: Expr.Ident, exprs: Array<Expr>, funcs: Array<Func>, scopes: List<Scope>, pos: FilePos
): Pair<Type, Errors> {
    val (arrayType, arrayErrors) = name.checkSemantics(funcs, scopes)
    val exprErrors = exprs.map { it.checkSemantics(funcs, scopes) }.flatMap { it.second }
    val (type, typeError) = arrayType.checkArrayType(exprs.size, pos)
    return type to arrayErrors + exprErrors + typeError
}

private fun checkIdent(name: String, scopes: List<Scope>, pos: FilePos): Pair<Type, Errors> =
    scopes.flatten().firstOrNull { it.first == name }?.let { it.second to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name, pos))

// </editor-fold>
