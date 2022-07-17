package com.taviak.printer_interface.ui.main

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.Data
import com.taviak.printer_interface.data.model.ValueType
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableFieldType
import com.taviak.printer_interface.util.CustomSpinner
import com.taviak.printer_interface.util.inflate
import kotlinx.android.synthetic.main.item_field_edittext.view.*
import kotlinx.android.synthetic.main.item_field_materialbutton.view.*
import kotlinx.android.synthetic.main.item_field_spinner.view.*

class MainFieldAdapter(
    private val variables: List<Variable>,
    private val data: Data,
    private val changeListener: (() -> Unit)? = null,
    private val onEdit: (Variable) -> Unit,
) : RecyclerView.Adapter<MainFieldAdapter.ViewHolder>() {

    var disableListeners = false
    var ignored = 0

    override fun getItemViewType(position: Int): Int =
        variables[position].field ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder = when (viewType) {
        VariableFieldType.EDITTEXT.ordinal -> EditTextViewHolder(parent.inflate(R.layout.item_field_edittext))
        VariableFieldType.MATERIAL_BUTTON.ordinal -> MaterialButtonViewHolder(parent.inflate(R.layout.item_field_materialbutton))
        else -> SpinnerViewHolder(parent.inflate(R.layout.item_field_spinner))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = variables[position]
        holder.setOnChangeListener {
            item.shortName?.let {
                data[it] = holder.getValue()
            }
            // TODO
            if (disableListeners) {
                ignored++
                if (ignored >= itemCount - 1) {
                    ignored = 0
                    disableListeners = false
                }
                return@setOnChangeListener
            }
            changeListener?.invoke()
        }
        holder.bind(item)
        item.shortName?.let {
            val value = data[it]
            if (!value.isNullOrBlank()) {
                holder.setValue(value)
            }
        }
    }

    override fun getItemCount() = variables.size

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: Variable)
        abstract fun getValue() : String?
        abstract fun setValue(value: String?)
        abstract fun setOnChangeListener(listener: () -> Unit)
    }

    inner class EditTextViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) = with(itemView) {
            text_edittext_field_name?.setOnClickListener {
                onEdit(item)
            }
            text_edittext_field_name?.text = item.name
            input_value?.inputType = if (item.valueType == ValueType.TEXT.ordinal) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            } else {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }

        override fun getValue(): String? = itemView.input_value?.text?.toString()
        override fun setValue(value: String?) {
            itemView.input_value?.setText(value)
        }

        override fun setOnChangeListener(listener: () -> Unit) {
            itemView.input_value?.addTextChangedListener {
                listener.invoke()
            }
        }
    }

    inner class MaterialButtonViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) = with(itemView) {
            text_materialbutton_field_name?.setOnClickListener {
                onEdit(item)
            }
            text_materialbutton_field_name?.text = item.name
            toggle_values?.removeAllViews()
            item.options.forEach { option ->
                val view = MaterialButton(context, null, R.attr.materialButtonOutlinedStyle)
                view.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1F
                }
                view.text = option
                toggle_values?.addView(view)
            }
        }

        override fun getValue(): String? {
            itemView.toggle_values?.checkedButtonId?.let {
                return itemView.findViewById<Button?>(it)?.text?.toString()
            }
            return null
        }
        override fun setValue(value: String?) {
            itemView.toggle_values?.children?.find {
                (it as Button?)?.text == value
            }?.id?.let { id ->
                itemView.toggle_values?.check(id)
            }
        }

        override fun setOnChangeListener(listener: () -> Unit) {
            itemView.toggle_values?.addOnButtonCheckedListener { _, _, _ ->
                listener.invoke()
            }
        }
    }

    inner class SpinnerViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) = with(itemView) {
            text_spinner_field_name?.setOnClickListener {
                onEdit(item)
            }
            text_spinner_field_name?.text = item.name
            spinner_value?.adapter = ArrayAdapter(
                context,
                R.layout.item_spinner,
                item.options
            )
        }

        override fun getValue(): String =
            variables[adapterPosition].options[itemView.spinner_value.selectedItemPosition]
        override fun setValue(value: String?) {
            val adapter = itemView.spinner_value?.adapter
            for (i in 0 .. (adapter?.count ?: 0)) {
                if (adapter?.getItem(i) == value) {
                    itemView.spinner_value?.setSelection(i)
                    break
                }
            }
        }

        override fun setOnChangeListener(listener: () -> Unit) {
            itemView.spinner_value?.setSpinnerEventsListener(object : CustomSpinner.OnSpinnerEventsListener {
                override fun onSpinnerOpened(spin: Spinner?) {}

                override fun onSpinnerClosed(spin: Spinner?) {
                    listener.invoke()
                }

            })
        }
    }
}