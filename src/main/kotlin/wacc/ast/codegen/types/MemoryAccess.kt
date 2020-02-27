package wacc.ast.codegen.types

enum class MemoryAccess {
    Byte {
        override fun toString(): String = "B"
    },
    HalfWord {
        override fun toString(): String = "H"
    },
    Word {
        override fun toString(): String = ""
    }
}
