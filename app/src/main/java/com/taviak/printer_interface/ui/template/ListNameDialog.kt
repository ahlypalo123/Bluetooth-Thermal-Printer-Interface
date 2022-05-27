package com.taviak.printer_interface.ui.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.taviak.printer_interface.R
import kotlinx.android.synthetic.main.dialog_list_name.*

class ListNameDialog(private val name: String) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_list_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        input_name?.setText(name)
        input_name?.addTextChangedListener {
            layout_name?.error = null
        }
        btn_apply?.setOnClickListener {
            val name = input_name?.text?.toString()
            if (name.isNullOrBlank()) {
                layout_name?.error = "Это поле не может быть пустым"
                return@setOnClickListener
            }
            (parentFragment as TemplateEditorFragment).setListName(name)
            dismiss()
        }
    }

}