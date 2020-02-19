package wacc.ast.codegen.types

// See ARM1176JZF-S Technical Reference Manual, table 1-15 for more info
enum class Condition {
    Equal,
    NotEqual,
    UnsignedHigherOrSame,
    CarrySet,
    UnsignedLower,
    CarryClear,
    Minus,
    Plus,
    Overflow,
    NoOverflow,
    UnsignedHigher,
    UnsignedLowerOrSame,
    SignedGreaterOrEqual,
    SignedLess,
    SignedGreaterThan,
    SignedLessOrEqual,
    Always
}
