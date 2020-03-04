package wacc.codegen.types

sealed class InitializedDatum {
    data class InitializedString(val label: String, val ascii: String) : InitializedDatum() {
        override fun toString(): String {
            return "$label:\n" +
                    "\t.word ${ascii.length + 1}\n" +
                    "\t.ascii \"${ascii.unescaped}\\0\""
        }
    }
}

private val String.unescaped: String
    get() = this.fold("") { acc, c ->
        when (c) {
            '\n' -> "$acc\\n"
            '\t' -> "$acc\\t"
            else -> "$acc$c"
        }
    }
