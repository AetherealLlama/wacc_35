package wacc.ast.codegen.types

// See ARM1176JZF-S Technical Reference Manual, table 1-15 for more info
enum class Condition(val display: String) {
    Equal("EQ"),
    NotEqual("NE"),
    UnsignedHigherOrSame("HS"),
    CarrySet("CS"),
    UnsignedLower("LO"),
    CarryClear("CC"),
    Minus("MI"),
    Plus("PL"),
    Overflow("VS"),
    NoOverflow("VC"),
    UnsignedHigher("HI"),
    UnsignedLowerOrSame("LS"),
    SignedGreaterOrEqual("GE"),
    SignedLess("LT"),
    SignedGreaterThan("GT"),
    SignedLessOrEqual("LE"),
    Always("");

    override fun toString(): String = display
}
