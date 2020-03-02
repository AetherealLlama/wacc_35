package wacc.codegen.types

sealed class Section {
    data class DataSection(val data: List<InitializedDatum>) : Section()
    data class TextSection(val instructions: List<List<Instruction>>) : Section()
}
