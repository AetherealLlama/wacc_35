package wacc.ast.codegen.types

sealed class InitializedDatum {
    data class InitializedString(val label: String, val length: Int, val ascii: String) : InitializedDatum() {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("$label:\n")
            builder.append(".word $length\n")
            builder.append(".ascii \"$ascii\"")
            return builder.toString()
        }
    }
}
