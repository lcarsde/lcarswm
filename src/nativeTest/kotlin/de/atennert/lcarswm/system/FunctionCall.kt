package de.atennert.lcarswm.system

class FunctionCall(val name: String, vararg parameterValues: Any?) {
    val parameters: List<Any?>

    init {
        this.parameters = listOf(parameterValues)
    }
}