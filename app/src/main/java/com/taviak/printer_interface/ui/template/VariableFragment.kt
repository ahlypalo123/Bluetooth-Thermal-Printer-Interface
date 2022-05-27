package com.taviak.printer_interface.ui.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.ValueType
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableScope
import com.taviak.printer_interface.util.gone
import com.taviak.printer_interface.util.inflate
import com.taviak.printer_interface.util.manageVisibleGone
import kotlinx.android.synthetic.main.fragment_variable.*
import kotlinx.android.synthetic.main.item_option.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VariableFragment(private var variable: Variable? = null) : Fragment() {

    private val items: MutableList<String> = mutableListOf()
    private val dao: VariableDao = App.db.variableDao()
    private val adapter = Adapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_variable, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("variable", variable)
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        savedInstanceState?.let {
            variable = it.getSerializable("variable") as Variable
        }

        radio_group_field_type?.setOnCheckedChangeListener { _, id ->
            val isEditText = id == R.id.btn_edittext
            text_value_type?.manageVisibleGone(isEditText)
            spinner_value_type?.manageVisibleGone(isEditText)
            text_options?.manageVisibleGone(!isEditText)
            list_options?.manageVisibleGone(!isEditText)
            btn_add_option?.manageVisibleGone(!isEditText)
            if (id == R.id.btn_material_button) {
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

        input_variable_name?.setText(variable?.name)
        input_variable_short_name?.setText(variable?.shortName)
        spinner_variable_type?.setSelection(variable?.scope ?: 0)
        radio_group_field_type?.check(
            radio_group_field_type?.children?.find {
                    it.tag.toString() == (variable?.field ?: 0).toString()
                }?.id!!
        )
        spinner_value_type?.setSelection(variable?.valueType ?: 0)
        variable?.let {
            items.addAll(it.options)
        }

        list_options?.layoutManager = LinearLayoutManager(context)
        list_options?.adapter = adapter

        btn_add_option?.setOnClickListener {
            items.add("")
            adapter.notifyDataSetChanged()
        }

        spinner_variable_type?.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            VariableScope.values().map { it.value }
        )

        spinner_value_type?.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner,
            ValueType.values().map { it.value }
        )

        btn_add?.setOnClickListener {
            if (radio_group_field_type?.checkedRadioButtonId == R.id.btn_material_button &&
                items.size > 4
            ) {
                scroll_view?.smoothScrollTo(0, 0)
                return@setOnClickListener
            }

            validateShortName {
                val field = view.findViewById<View>(
                    radio_group_field_type?.checkedRadioButtonId!!
                ).tag.toString().toIntOrNull()

                val v = Variable(
                    name = input_variable_name?.text?.toString(),
                    shortName = input_variable_short_name?.text?.toString(),
                    scope = spinner_variable_type?.selectedItemPosition,
                    field = field,
                    valueType = spinner_value_type?.selectedItemPosition,
                    expression = "",
                    options = items
                )

                CoroutineScope(Dispatchers.IO).launch {
                    if (variable == null) {
                        dao.insert(v)
                    } else {
                        dao.save(v)
                    }
                    withContext(Dispatchers.Main) {
                        activity?.supportFragmentManager?.popBackStack()
                    }
                }
            }
        }

        input_variable_short_name?.addTextChangedListener {
            input_variable_short_name?.error = null
        }
    }

    private fun getViewHolderForPosition(position: Int) : Adapter.ViewHolder? =
        list_options?.findViewHolderForAdapterPosition(position) as Adapter.ViewHolder?

    private fun validateShortName(callback: () -> Unit) {
        val name = input_variable_short_name?.text?.toString()
        if (name.isNullOrBlank()) {
            input_variable_short_name?.error = "Это поле не может быть пустым"
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val variable = dao.findByShortName(name)
            withContext(Dispatchers.Main) {
                if (variable == null) {
                    callback()
                } else {
                    input_variable_short_name?.error =
                        "Переменная с таким коротким названием уже существует"
                }
            }
        }
    }

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