package wacc.ast.codegen.types

sealed class Section {
    data class DataSection(val data: List<InitializedDatum>) : Section()
    data class TextSection(val functions: List<Function>) : Section()
}
