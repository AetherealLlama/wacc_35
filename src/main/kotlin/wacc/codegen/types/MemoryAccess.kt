package wacc.codegen.types

enum class MemoryAccess(private val display: String) {
    Byte("B"),
    SignedByte("SB"),
    Word("");

    override fun toString(): String = display
}
