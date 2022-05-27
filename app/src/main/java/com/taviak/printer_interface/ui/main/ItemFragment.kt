package com.taviak.printer_interface.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.ReceiptItem
import com.taviak.printer_interface.data.model.ReceiptTemplateData
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.util.getCallerFragment
import com.taviak.printer_interface.util.manageVisibleGone
import kotlinx.android.synthetic.main.fragment_item.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemFragment(
    private val variables: List<Variable> = mutableListOf()
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list_fields?.layoutManager = LinearLayoutManager(context)
        list_fields?.adapter = FieldAdapter(variables)

        text_empty?.manageVisibleGone(variables.isEmpty())

        btn_add?.setOnClickListener {
            val map: ReceiptItem = mutableMapOf()
            for (i in 0 until list_fields?.size!!) {
                val value = (list_fields?.findViewHolderForAdapterPosition(i)
                        as FieldAdapter.ViewHolder?)?.getValue()
                val name = variables[i].shortName!!
                map?.set(name, value)
            }
            (activity?.supportFragmentManager?.getCallerFragment() as MainFragment).addItem(map)
            activity?.supportFragmentManager?.popBackStack()
        }
    }

}