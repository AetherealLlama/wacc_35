package wacc.codegen.types

sealed class InitializedDatum {
    data class InitializedString(val label: String, val length: Int, val ascii: String) : InitializedDatum() {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("$label:\n")
            builder.append("\t.word ${length + 1}\n")
            builder.append("\t.ascii \"$ascii\\0\"")
            return builder.toString()
        }
    }
}
