package com.taviak.printer_interface.ui.main

import android.content.Context
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.Data
import com.taviak.printer_interface.data.model.ListData
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableFieldType
import com.taviak.printer_interface.util.readRawContent
import java.util.concurrent.Executors
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class MainScriptPresenter(
    private val variables: List<Variable>,
    private val view: MainScriptView
) {
    private val engine: ScriptEngine = ScriptEngineManager().getEngineByName("rhino")
    private val executor = Executors.newSingleThreadExecutor()

    fun init(context: Context?) {
        engine.eval(context?.readRawContent(R.raw.base_functions))
    }

    fun onVariablesUpdated() = executor.execute {
        variables.forEach { variable ->
            engine.put(variable.shortName, view.fieldData[variable.shortName])
            view.listData.forEach { data ->
                data.value
                    .flatMap { it.asSequence() }
                    .groupBy({ it.key }, { it.value })
                    .forEach {
                        engine.put(it.key, it.value)
                    }
                engine.put(data.key, data.value)
            }
        }
        variables.forEach { variable ->
            if (variable.expression?.isNotBlank() == true && variable.field == VariableFieldType.EDITTEXT.ordinal) {
                view.fieldData[variable.shortName!!] = engine.eval(variable.expression)?.toString()
            }
        }
        view.onExpressionEvaluated()
    }

}