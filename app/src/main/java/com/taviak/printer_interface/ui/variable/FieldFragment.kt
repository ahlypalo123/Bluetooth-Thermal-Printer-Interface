package com.taviak.printer_interface.ui.variable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.ValueType
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableFieldType
import com.taviak.printer_interface.util.*
import kotlinx.android.synthetic.main.fragment_field.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FieldFragment(
    private var forItem: Boolean,
    private var variable: Variable
) : Fragment() {

    private val dao: VariableDao = App.db.variableDao()
    private val items: MutableList<String> = mutableListOf()
    private val adapter = Adapter()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("forItem", forItem)
        outState.putSerializable("variable", variable)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_field, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_hint?.manageVisibleGone(activity?.supportFragmentManager?.getCallerFragment()
                is VariableListFragment)

        savedInstanceState?.let {
            forItem = it.getBoolean("forItem")
            variable = it.getSerializable("variable") as Variable
        }

        spinner_field_type.setSpinnerEventsListener(object : CustomSpinner.OnSpinnerEventsListener {
            override fun onSpinnerOpened(spin: Spinner?) {

            }

            override fun onSpinnerClosed(spin: Spinner?) {
                updateUi()
            }
        })

        spinner_field_type?.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            VariableFieldType.values().map { it.value }
        )

        input_variable_name?.setText(variable.name)
        spinner_field_type?.setSelection(variable.field ?: 0)
        toggle_value_type?.check(variable.valueType ?: ValueType.TEXT.ordinal)
        items.addAll(variable.options)

        list_options?.layoutManager = LinearLayoutManager(context)
        list_options?.adapter = adapter

        btn_add_option?.setOnClickListener {
            items.add("")
            adapter.notifyDataSetChanged()
        }

        btn_add?.setOnClickListener {
            val fieldType = spinner_field_type?.selectedItemPosition
            val valueType = toggle_value_type?.checkedButtonId?.let { id ->
                view.findViewById<View?>(id)
                    ?.tag?.toString()?.toInt()
            }
            if (fieldType == VariableFieldType.MATERIAL_BUTTON.ordinal && items.size > 4) {
                scroll_view?.smoothScrollTo(0, 0)
                return@setOnClickListener
            }

            val v = Variable(
                id = variable.id,
                name = input_variable_name?.text?.toString(),
                shortName = variable.shortName,
                scope = variable.scope,
                field = fieldType,
                valueType = valueType,
                expression = input_expression?.text?.toString(),
                options = items
            )

            CoroutineScope(Dispatchers.IO).launch {
                dao.save(v)
                withContext(Dispatchers.Main) {
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        }

        updateUi()
    }

    private fun updateUi() {
        val isEditText = spinner_field_type?.selectedItemPosition == VariableFieldType.EDITTEXT.ordinal
        text_value_type?.manageVisibleGone(isEditText)
        toggle_value_type?.manageVisibleGone(isEditText)
        input_expression?.manageVisibleGone(isEditText)
        text_expression?.manageVisibleGone(isEditText)
        text_options?.manageVisibleGone(!isEditText)
        list_options?.manageVisibleGone(!isEditText)
        btn_add_option?.manageVisibleGone(!isEditText)
        if (spinner_field_type?.selectedItemPosition == VariableFieldType.MATERIAL_BUTTON.ordinal) {
            btn_add_option?.manageVisibleGone(items.size < 4)
            if (items.size > 4) {
                for (i in 4 until items.size) {
                    getViewHolderForPosition(i)?.setError()
                }
            }
        } else {
            for (i in 0 until items.size) {
                getViewHolderForPosition(i)?.clearError()
            }
        }
    }

    private fun getViewHolderForPosition(position: Int) : Adapter.ViewHolder? =
        list_options?.findViewHolderForAdapterPosition(position) as Adapter.ViewHolder?

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_option))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(text: String) = with(itemView) {
                input_option?.setText(text)
                input_option?.addTextChangedListener {
                    items[adapterPosition] = it.toString()
                }
                btn_delete?.setOnClickListener {
                    items.remove(text)
                    notifyItemRemoved(adapterPosition)
                }
                if (adapterPosition == itemCount - 1) {
                    input_option?.requestFocus()
                }
            }

            fun setError() {
                itemView.input_option?.error =
                    "У данного типа поля не может быть более 4х вариантов выбора"
            }

            fun clearError() {
                itemView.input_option?.error = null
            }

        }
    }

}