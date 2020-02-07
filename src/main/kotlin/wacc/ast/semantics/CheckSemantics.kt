package wacc.ast.semantics

import wacc.ast.*


internal typealias Scope = List<Pair<String, Type>>
internal typealias Errors = List<SemanticError>


fun Program.checkSemantics(): Errors =
    funcs.flatMap { it.checkSemantics(SemanticContext(funcs, it, true)) } +
            stat.checkSemantics(SemanticContext(funcs, null, false).withNewScope()).second

private fun Func.checkSemantics(ctx: SemanticContext): Errors =
        stat.checkSemantics(ctx.withNewScope(params.map { it.name to it.type })).second

private fun Stat.checkSemantics(ctx: SemanticContext): Pair<Scope, Errors> = when(this) {
    is Stat.Skip -> ctx.currentScope to emptyList()
    is Stat.AssignNew -> {
        val (rhsType, rhsErrors) = rhs.checkSemantics(ctx)
        val typeError = if (!(rhsType matches type)) listOf(TypeMismatch(type, rhsType, pos))
                        else emptyList<SemanticError>()
        if (name in ctx.currentScope.map { it.first })
            ctx.currentScope to rhsErrors + typeError + listOf(DuplicateDeclarationError(name, pos))
        else
            (ctx.currentScope + (name to type)) to rhsErrors + typeError
    }
    is Stat.Assign -> {
        val (lhsType, lhsErrors) = lhs.checkSemantics(ctx)
        val (rhsType, rhsErrors) = rhs.checkSemantics(ctx)
        var typeError = emptyList<SemanticError>()
        if (!(lhsType matches rhsType))
            typeError = listOf(TypeMismatch(lhsType, rhsType, pos))
        ctx.currentScope to lhsErrors + rhsErrors + typeError
    }
    is Stat.Read -> lhs.checkSemantics(ctx).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.BaseType.TypeInt, Type.BaseType.TypeChar).none { type matches it })
            typeError = listOf(ReadTypeMismatch(type, pos))
        ctx.currentScope to errors + typeError
    }
    is Stat.Free -> expr.checkSemantics(ctx).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.ArrayType(Type.AnyType), ANY_PAIR).none { type matches it })
            typeError = listOf(FreeTypeMismatch(type, pos))
        ctx.currentScope to errors + typeError
    }
    is Stat.Return -> expr.checkSemantics(ctx).let { (type, expErrors) ->
        val returnError =
                ctx.func?.let { if (type matches it.type) emptyList() else listOf(ReturnTypeMismatch(it, type, pos)) }
                        ?: listOf(ReturnOutsideFuncError(pos))
        ctx.currentScope to expErrors + returnError
    }
    is Stat.Exit -> expr.checkSemantics(ctx).let { (type, expErrors) ->
        val typeError = if (type matches Type.BaseType.TypeInt) emptyList<SemanticError>()
                        else listOf(ExitTypeMismatch(type, pos))
        ctx.currentScope to expErrors + typeError
    }
    is Stat.Print,
    is Stat.Println -> ctx.currentScope to emptyList()
    is Stat.IfThenElse -> {
        val exprErrors = expr.checkBool(ctx)
        val (_, branch1Errors) = branch1.checkSemantics(ctx.withNewScope())
        val (_, branch2Errors) = branch2.checkSemantics(ctx.withNewScope())
        ctx.currentScope to exprErrors + branch1Errors + branch2Errors
    }
    is Stat.WhileDo -> {
        val exprErrors = expr.checkBool(ctx)
        val (_, statErrors) = stat.checkSemantics(ctx.withNewScope())
        ctx.currentScope to exprErrors + statErrors
    }
    is Stat.Begin -> ctx.currentScope to stat.checkSemantics(ctx.withNewScope()).second
    is Stat.Compose -> {
        val (scope1, stat1Errors) = stat1.checkSemantics(ctx.withLastStat(false))
        val (scope2, stat2Errors) = stat2.checkSemantics(ctx.withModifiedScope(scope1))
        scope2 to (stat1Errors + stat2Errors)
    }
}.let { (scope, errors) -> scope to errors + checkLastStatement(ctx) }

private fun Expr.checkSemantics(ctx: SemanticContext): Pair<Type, Errors> = when(this) {
    is Expr.Literal.IntLiteral -> Type.BaseType.TypeInt to emptyList()
    is Expr.Literal.BoolLiteral -> Type.BaseType.TypeBool to emptyList()
    is Expr.Literal.CharLiteral -> Type.BaseType.TypeChar to emptyList()
    is Expr.Literal.StringLiteral -> Type.BaseType.TypeString to emptyList()
    is Expr.Literal.PairLiteral -> ANY_PAIR to emptyList()
    is Expr.Ident -> checkIdent(name, ctx, pos)
    is Expr.ArrayElem -> checkArrayElem(name, exprs, ctx, pos)
    is Expr.UnaryOp -> expr.checkSemantics(ctx).let { (type, errors) ->
        var argTypeError = emptyList<SemanticError>()
        if (!(type matches operator.argType)) argTypeError = listOf(UnaryOpInvalidType(type, operator, pos))
        operator.returnType to errors + argTypeError
    }
    is Expr.BinaryOp -> {
        val (arg1Type, arg1Errors) = expr1.checkSemantics(ctx)
        val (arg2Type, arg2Errors) = expr2.checkSemantics(ctx)
        val errors = mutableListOf<SemanticError>().apply { addAll(arg1Errors); addAll(arg2Errors) }
        // Args must match possible function's accepted types
        if (operator.argTypes.none { arg1Type matches it }) errors += BinaryOpInvalidType(arg1Type, operator, pos)
        // Args must match eachother
        if (!(arg1Type matches arg2Type)) errors += BinaryArgsMismatch(arg1Type, arg2Type, operator, pos)
        operator.returnType to errors
    }
}

private fun AssignLhs.checkSemantics(ctx: SemanticContext): Pair<Type, Errors> = when(this) {
    is AssignLhs.Variable -> checkIdent(name, ctx, pos)
    is AssignLhs.ArrayElem -> checkArrayElem(Expr.Ident(pos, name), exprs, ctx, pos)
    is AssignLhs.PairElem -> expr.checkPairElem(ctx)(accessor)
}

private fun AssignRhs.checkSemantics(ctx: SemanticContext): Pair<Type, Errors> = when(this) {
    is AssignRhs.Expression -> expr.checkSemantics(ctx)
    is AssignRhs.ArrayLiteral -> {
        val checkedExprs = exprs.map { it.checkSemantics(ctx) }
        val errors = checkedExprs.flatMap { it.second }
        val type = Type.ArrayType(checkedExprs.firstOrNull()?.first ?: Type.AnyType)
        type to errors
    }
    is AssignRhs.Newpair -> {
        val (fstRawType, fstErrors) = expr1.checkSemantics(ctx)
        val (sndRawType, sndErrors) = expr2.checkSemantics(ctx)
        // Make sure elems are types that can be put in pairs
        val (fstType, fstTypeError) = fstRawType.asPairElemType?.let { it to emptyList<SemanticError>() }
                ?: Type.AnyType to listOf(InvalidPairElemType(fstRawType, pos))
        val (sndType, sndTypeError) = sndRawType.asPairElemType?.let { it to emptyList<SemanticError>() }
                ?: Type.AnyType to listOf(InvalidPairElemType(sndRawType, pos))
        Type.PairType(fstType, sndType) to listOf(fstErrors, sndErrors, fstTypeError, sndTypeError).flatten()
    }
    is AssignRhs.PairElem -> expr.checkPairElem(ctx)(accessor)
    is AssignRhs.Call -> ctx.funcs.find { it.name == name }?.let { it.type to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name, pos))
}

// <editor-fold desc="Common type checkers, to keep things dry">

private fun Expr.checkBool(ctx: SemanticContext): Errors {
    val (exprType, exprErrors) = checkSemantics(ctx)
    return if (exprType.matches(Type.BaseType.TypeBool))
        exprErrors
    else
        exprErrors + TypeMismatch(Type.BaseType.TypeBool, exprType, pos)
}

private fun Expr.checkPairElem(ctx: SemanticContext): (PairAccessor) -> Pair<Type, Errors> {
    val (exprType, exprErrors) = checkSemantics(ctx)
    return if (exprType matches ANY_PAIR)
        { accessor -> when (accessor) {
            PairAccessor.FST -> (exprType as Type.PairType).type1
            PairAccessor.SND -> (exprType as Type.PairType).type2
        }.let { pairElemType -> pairElemType.asNormalType to exprErrors } }
    else
        { _ -> Type.AnyType to exprErrors + TypeMismatch(ANY_PAIR, exprType, pos) }
}

private fun checkArrayElem(
        name: Expr.Ident, exprs: Array<Expr>, ctx: SemanticContext, pos: FilePos
): Pair<Type, Errors> {
    val (arrayType, arrayErrors) = name.checkSemantics(ctx)
    val exprErrors = exprs.map { it.checkSemantics(ctx) }.flatMap { it.second }
    val (type, typeError) = arrayType.checkArrayType(exprs.size, pos)
    return type to arrayErrors + exprErrors + typeError
}

private fun checkIdent(name: String, ctx: SemanticContext, pos: FilePos): Pair<Type, Errors> =
    ctx.scopes.flatten().firstOrNull { it.first == name }?.let { it.second to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name, pos))

private fun Stat.checkLastStatement(ctx: SemanticContext): Errors = if (ctx.isLastStat) {
    val getError = ctx.func?.let { { FunctionEndError(it.name, pos) } } ?: { ProgramEndError(pos) }
    when(this) {
        is Stat.Return -> ctx.func?.let { emptyList<SemanticError>() } ?: listOf(getError())
        is Stat.IfThenElse,
        is Stat.WhileDo,
        is Stat.Begin,
        is Stat.Compose,
        is Stat.Exit -> emptyList()
        is Stat.Skip,
        is Stat.AssignNew,
        is Stat.Assign,
        is Stat.Read,
        is Stat.Free,
        is Stat.Print,
        is Stat.Println -> listOf(getError())
    }
} else emptyList()

// </editor-fold>
