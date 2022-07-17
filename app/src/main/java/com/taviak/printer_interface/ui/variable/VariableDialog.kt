package com.taviak.printer_interface.ui.variable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableFieldType
import com.taviak.printer_interface.data.model.VariableScope
import com.taviak.printer_interface.util.manageVisibleGone
import kotlinx.android.synthetic.main.dialog_variable.*
import kotlinx.android.synthetic.main.dialog_variable.btn_add
import kotlinx.android.synthetic.main.dialog_variable.input_variable_name
import kotlinx.android.synthetic.main.dialog_variable.spinner_variable_type
import kotlinx.android.synthetic.main.dialog_variable.text_variable_type
import kotlinx.android.synthetic.main.fragment_field.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VariableDialog(
    private var forItem: Boolean,
    private var variable: Variable?
) : DialogFragment() {

    private val dao: VariableDao = App.db.variableDao()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_variable, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input_variable_name?.setText(variable?.shortName)

        spinner_variable_type?.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            VariableScope.values().map { it.value }
        )

        spinner_variable_type?.manageVisibleGone(forItem)
        text_variable_type?.manageVisibleGone(forItem)

        spinner_variable_type?.setSelection(if (forItem) {
            VariableScope.ITEM.ordinal
        } else {
            variable?.scope ?: 0
        })

        input_variable_name?.addTextChangedListener {
            input_variable_name?.error = null
        }

        btn_add?.setOnClickListener {
            saveVariable {
                dismiss()
            }
        }

        btn_edit_field?.setOnClickListener {
            saveVariable {
                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left,
                        R.anim.pop_enter, R.anim.pop_exit)
                    ?.replace(R.id.layout_activity_container, FieldFragment(false, variable!!))
                    ?.addToBackStack(null)?.commit()
                dismiss()
            }
        }
    }

    private fun saveVariable(onSaved: () -> Unit) {
        val shortName = input_variable_name?.text?.toString()
        val scope = spinner_variable_type?.selectedItemPosition

        if (!validateShortNameFormat()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (!validateShortNameUnique(shortName!!)) {
                return@launch
            }
            if (variable == null) {
                val v = Variable(
                    name = shortName,
                    shortName = shortName,
                    scope = scope,
                )
                v.id = dao.insert(v)
                variable = v
            } else {
                variable?.shortName = shortName
                variable?.scope = scope
                dao.save(variable!!)
            }
            withContext(Dispatchers.Main) {
                onSaved()
            }
        }


    }

    private fun validateShortNameFormat() : Boolean {
        val name = input_variable_name?.text?.toString()
        if (name.isNullOrBlank()) {
            input_variable_name?.error = "Это поле не может быть пустым"
            return false
        }
        if (!Regex("\\w+").matches(name)) {
            input_variable_name?.error =
                "Поле не должно содержать пробелов или специальных символов"
            return false
        }
        return true
    }

    private suspend fun validateShortNameUnique(name: String) : Boolean {
        val variable = dao.findByShortName(name)
        if (variable != null) {
            withContext(Dispatchers.Main) {
                input_variable_name?.error =
                    "Переменная с таким названием уже существует"
            }
            return false
        }
        return true
    }

}