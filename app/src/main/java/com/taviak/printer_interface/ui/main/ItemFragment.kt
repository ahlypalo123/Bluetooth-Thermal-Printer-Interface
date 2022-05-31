package com.taviak.printer_interface.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.fragment_item.toolbar
import kotlinx.android.synthetic.main.fragment_template.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemFragment(
    private val variables: List<Variable> = mutableListOf()
) : Fragment() {

    private var fieldData: MutableMap<String, String?> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        setHasOptionsMenu(true)

        list_fields?.layoutManager = LinearLayoutManager(context)
        list_fields?.adapter = FieldAdapter(variables, fieldData)

        text_empty?.manageVisibleGone(variables.isEmpty())

        btn_add?.setOnClickListener {
            (activity?.supportFragmentManager?.getCallerFragment() as MainFragment?)
                ?.addItem(fieldData)
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.btn_menu_close) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

}