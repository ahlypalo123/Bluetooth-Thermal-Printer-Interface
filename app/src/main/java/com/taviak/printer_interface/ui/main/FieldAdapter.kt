package com.taviak.printer_interface.ui.main

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.util.inflate
import kotlinx.android.synthetic.main.item_field_edittext.view.*
import kotlinx.android.synthetic.main.item_field_materialbutton.view.*
import kotlinx.android.synthetic.main.item_field_spinner.view.*

class FieldAdapter(
    private val variables: List<Variable>
) : RecyclerView.Adapter<FieldAdapter.ViewHolder>() {

    override fun getItemViewType(position: Int): Int =
        variables[position].field ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder = when (viewType) {
        0 -> EditTextViewHolder(parent.inflate(R.layout.item_field_edittext))
        1 -> MaterialButtonViewHolder(parent.inflate(R.layout.item_field_materialbutton))
        else -> SpinnerViewHolder(parent.inflate(R.layout.item_field_spinner))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(variables[position])

    override fun getItemCount() = variables.size

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: Variable)
        abstract fun getValue() : String?
    }

    inner class EditTextViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) {
            itemView.text_edittext_field_name.text = item.name
        }

        override fun getValue(): String? = itemView.input_value?.text?.toString()
    }

    inner class MaterialButtonViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) {
            itemView.text_materialbutton_field_name?.text = item.name
            itemView.toggle_values?.removeAllViews()
            item.options.forEach { option ->
                val view = MaterialButton(itemView.context, null, R.attr.materialButtonOutlinedStyle)
                view.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1F
                }
                view.text = option
                itemView.toggle_values?.addView(view)
            }
        }

        override fun getValue(): String? {
            itemView.toggle_values?.checkedButtonId?.let {
                return itemView.findViewById<Button?>(it)?.text.toString()
            }
            return null
        }
    }

    inner class SpinnerViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun bind(item: Variable) {
            itemView.text_spinner_field_name?.text = item.name
            itemView.spinner_value?.adapter = ArrayAdapter(
                itemView.context,
                R.layout.item_spinner,
                item.options
            )
        }

        override fun getValue(): String =
            variables[adapterPosition].options[itemView.spinner_value.selectedItemPosition]
    }
}