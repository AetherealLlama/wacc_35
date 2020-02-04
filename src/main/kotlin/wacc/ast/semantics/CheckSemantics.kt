package wacc.ast.semantics

import wacc.ast.*


typealias Scope = List<Pair<String, Type>>
typealias Errors = List<SemanticError>


fun Program.checkSemantics(): Errors =
    funcs.flatMap { it.checkSemantics(funcs) } + stat.checkSemantics(funcs).second

private fun Func.checkSemantics(funcs: Array<Func>): Errors {
    var lastStatementError = emptyList<SemanticError>()
    var lastStat = stat
    while (lastStat is Stat.Compose)
        lastStat = lastStat.stat2
    if (lastStat !is Stat.Exit && lastStat !is Stat.Compose)
        lastStatementError = listOf(FunctionEndError(name))
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
            typeError = listOf(TypeMismatch(type, rhsType))
        if (name in scopes.flatten().map { it.first })
            currentScope to rhsErrors + typeError + listOf(DuplicateDeclarationError(name))
        else
            (currentScope + (name to type)) to rhsErrors + typeError
    }
    is Stat.Assign -> {
        val (lhsType, lhsErrors) = lhs.checkSemantics(funcs, listOf(currentScope) + scopes)
        val (rhsType, rhsErrors) = rhs.checkSemantics(funcs, listOf(currentScope) + scopes)
        var typeError = emptyList<SemanticError>()
        if (!(lhsType matches rhsType))
            typeError = listOf(TypeMismatch(lhsType, rhsType))
        currentScope to lhsErrors + rhsErrors + typeError
    }
    is Stat.Read -> lhs.checkSemantics(funcs, listOf(currentScope) + scopes).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.BaseType.TypeInt, Type.BaseType.TypeChar).none { type matches it })
            typeError = listOf(ReadTypeMismatch(type))
        currentScope to errors + typeError
    }
    is Stat.Free -> expr.checkSemantics(funcs, scopes).let { (type, errors) ->
        var typeError = emptyList<SemanticError>()
        if (listOf(Type.ArrayType(Type.AnyType), Type.PairType(Type.AnyType, Type.AnyType)).none { type matches it })
            typeError = listOf(FreeTypeMismatch(type))
        currentScope to errors + typeError
    }
    is Stat.Return,
    is Stat.Exit,
    is Stat.Print,
    is Stat.Println -> currentScope to emptyList()
    is Stat.IfThenElse -> {
        val (exprType, exprErrors) = expr.checkSemantics(funcs, listOf(currentScope) + scopes)
        val exprTypeError =
                if (exprType.matches(Type.BaseType.TypeBool)) emptyList<SemanticError>()
                else listOf(TypeMismatch(Type.BaseType.TypeBool, exprType))
        val (_, branch1Errors) = branch1.checkSemantics(funcs, listOf(currentScope) + scopes)
        val (_, branch2Errors) = branch2.checkSemantics(funcs, listOf(currentScope) + scopes)
        currentScope to listOf(exprErrors, exprTypeError, branch1Errors, branch2Errors).flatten()
    }
    is Stat.WhileDo -> {
        val (exprType, exprErrors) = expr.checkSemantics(funcs, listOf(currentScope) + scopes)
        val exprTypeError =
                if (exprType.matches(Type.BaseType.TypeBool)) emptyList<SemanticError>()
                else listOf(TypeMismatch(Type.BaseType.TypeBool, exprType))
        val (_, statErrors) = stat.checkSemantics(funcs, listOf(currentScope) + scopes)
        currentScope to listOf(exprErrors, exprTypeError, statErrors).flatten()
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
    is Expr.Literal.PairLiteral -> Type.PairType(Type.AnyType, Type.AnyType) to emptyList()
    is Expr.Ident -> scopes.flatten().firstOrNull { it.first == name }?.let { it.second to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name))
    is Expr.ArrayElem ->
        name.checkSemantics(funcs, scopes).let { (arrayType, arrayErrors) ->
            val checkedExprs = exprs.map { it.checkSemantics(funcs, scopes) }
            val errors = arrayErrors.toMutableList()
            errors.addAll(checkedExprs.flatMap { it.second })
            val type = if (arrayType !is Type.AnyType) TODO() else Type.AnyType
            type to errors
        }
    is Expr.UnaryOp -> expr.checkSemantics(funcs, scopes).let { (type, errors) ->
        var argTypeError = emptyList<SemanticError>()
        if (!(type matches operator.argType)) argTypeError = listOf(UnaryOpInvalidType(type, operator))
        operator.returnType to errors + argTypeError
    }
    is Expr.BinaryOp -> {
        val (arg1Type, arg1Errors) = expr1.checkSemantics(funcs, scopes)
        val (arg2Type, arg2Errors) = expr2.checkSemantics(funcs, scopes)
        val errors = mutableListOf<SemanticError>().apply { addAll(arg1Errors); addAll(arg2Errors) }
        // Args must match possible function's accepted types
        if (operator.argTypes.none { arg1Type matches it }) errors += BinaryOpInvalidType(arg1Type, operator)
        // Args must match eachother
        if (!(arg1Type matches arg2Type)) errors += BinaryArgsMismatch(arg1Type, arg2Type, operator)
        operator.returnType to errors
    }
}

private fun AssignLhs.checkSemantics(funcs: Array<Func>, scopes: List<Scope>): Pair<Type, Errors> = when(this) {
    is AssignLhs.Variable -> scopes.flatten().firstOrNull { it.first == name }?.let { it.second to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name))  // TODO: collapse dupe
    is AssignLhs.ArrayElem -> Expr.Ident(name).checkSemantics(funcs, scopes).let { (arrayType, arrayErrors) ->
        val checkedExprs = exprs.map { it.checkSemantics(funcs, scopes) }
        val errors = arrayErrors.toMutableList()
        errors.addAll(checkedExprs.flatMap { it.second })
        val type = if (arrayType !is Type.AnyType) TODO() else Type.AnyType
        type to errors
    }
    is AssignLhs.PairElem -> expr.checkSemantics(funcs, scopes).let { (exprType, exprErrors) ->
        if (exprType matches Type.PairType(Type.AnyType, Type.AnyType))
            when (accessor) {
                PairAccessor.FST -> (exprType as Type.PairType).type1
                PairAccessor.SND -> (exprType as Type.PairType).type2
            }.let { type -> type as Type to exprErrors }
        else Type.AnyType to exprErrors + TypeMismatch(Type.PairType(Type.AnyType, Type.AnyType), exprType)
    }
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
                    Type.AnyType to listOf(InvalidPairElemType(fstRawType))
        val (sndType, sndTypeError) =
                if (sndRawType is Type.PairElemType)
                    (sndRawType as Type.PairElemType) to emptyList<SemanticError>()
                else
                    Type.AnyType to listOf(InvalidPairElemType(sndRawType))
        Type.PairType(fstType, sndType) to listOf(fstErrors, sndErrors, fstTypeError, sndTypeError).flatten()
    }
    is AssignRhs.PairElem -> expr.checkSemantics(funcs, scopes).let { (exprType, exprErrors) ->
        if (exprType matches Type.PairType(Type.AnyType, Type.AnyType))
            when (accessor) {
                PairAccessor.FST -> (exprType as Type.PairType).type1
                PairAccessor.SND -> (exprType as Type.PairType).type2
            }.let { type -> type as Type to exprErrors }
        else Type.AnyType to exprErrors + TypeMismatch(Type.PairType(Type.AnyType, Type.AnyType), exprType)
    }
    is AssignRhs.Call -> funcs.find { it.name == name }?.let { it.type to emptyList<SemanticError>() }
            ?: Type.AnyType to listOf(IdentNotFoundError(name))
}

private val unaryOpTypes: Map<UnaryOperator, Pair<Type, Type>> = mapOf(
        UnaryOperator.BANG  to (Type.BaseType.TypeBool to Type.BaseType.TypeBool),
        UnaryOperator.MINUS to (Type.BaseType.TypeInt to Type.BaseType.TypeInt),
        UnaryOperator.LEN   to (Type.ArrayType(Type.AnyType) to Type.BaseType.TypeInt),
        UnaryOperator.ORD   to (Type.BaseType.TypeChar to Type.BaseType.TypeInt),
        UnaryOperator.CHR   to (Type.BaseType.TypeInt  to Type.BaseType.TypeChar)
)

private val pairElemTypes = listOf(
        Type.BaseType.TypeInt,
        Type.BaseType.TypeBool,
        Type.BaseType.TypeChar,
        Type.BaseType.TypeString,
        Type.PairPairElem
)

infix fun Type.matches(other: Type): Boolean {
    if (this is Type.AnyType || other is Type.AnyType) return true
    if (this is Type.ArrayType && other is Type.ArrayType) return this.type matches other.type
    if (this is Type.PairType && other is Type.PairType) {
        return (this.type1.normalType matches other.type1.normalType)
                && (this.type2.normalType matches other.type2.normalType)
    }
    return this.javaClass == other.javaClass
}

private val Type.PairElemType.normalType: Type
    get() = if (this is Type.PairPairElem) Type.PairType(Type.AnyType, Type.AnyType) else this as Type

val UnaryOperator.argType: Type
    get() = unaryOpTypes.getValue(this).first

private val UnaryOperator.returnType: Type
    get() = unaryOpTypes.getValue(this).second

val BinaryOperator.argTypes: List<Type>
    get() = when(this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB -> listOf(Type.BaseType.TypeInt)
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE -> listOf(Type.BaseType.TypeInt, Type.BaseType.TypeChar)
        BinaryOperator.EQ,
        BinaryOperator.NEQ -> listOf(Type.BaseType.TypeInt, Type.BaseType.TypeBool, Type.BaseType.TypeChar,
                Type.ArrayType(Type.AnyType), Type.PairType(Type.AnyType, Type.AnyType))
        BinaryOperator.LAND,
        BinaryOperator.LOR -> listOf(Type.BaseType.TypeBool)
    }

val BinaryOperator.returnType: Type
    get() = when(this) {
        BinaryOperator.MUL,
        BinaryOperator.DIV,
        BinaryOperator.MOD,
        BinaryOperator.ADD,
        BinaryOperator.SUB -> Type.BaseType.TypeInt
        BinaryOperator.GT,
        BinaryOperator.GTE,
        BinaryOperator.LT,
        BinaryOperator.LTE,
        BinaryOperator.EQ,
        BinaryOperator.NEQ,
        BinaryOperator.LAND,
        BinaryOperator.LOR -> Type.BaseType.TypeBool
    }