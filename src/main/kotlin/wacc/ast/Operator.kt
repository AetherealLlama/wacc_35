package wacc.ast

/**
 * Unary operators used in expressions
 */
enum class UnaryOperator {
    BANG,
    MINUS,
    LEN,
    ORD,
    CHR,
    BNOT
}

/**
 * Binary operators used in expressions
 */
enum class BinaryOperator {
    MUL,
    DIV,
    MOD,
    ADD,
    SUB,
    GT,
    GTE,
    LT,
    LTE,
    EQ,
    NEQ,
    LAND,
    LOR,
    BAND,
    BOR,
    BXOR,
    BLEFT,
    BRIGHT
}
