package wacc.ast

enum class UnaryOperator {
    BANG,
    MINUS,
    LEN,
    ORD,
    CHR
}

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
    LOR
}