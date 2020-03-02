package wacc.codegen.types

import wacc.codegen.types.Instruction.Special.Label

data class Function(val label: Label, val instructions: List<Instruction>, val main: Boolean = false) {
    override fun toString(): String {
        val builder = StringBuilder()
        if (main) {
            // TODO find a way to use the Global special instruction?
            builder.append(".global ${label.name}\n")
        }
        instructions.forEach { builder.appendln(it) }
        return builder.toString()
    }
}
