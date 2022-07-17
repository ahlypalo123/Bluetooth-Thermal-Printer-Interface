package com.taviak.printer_interface.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.Data
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.ui.variable.FieldFragment
import com.taviak.printer_interface.util.getCallerFragment
import com.taviak.printer_interface.util.manageVisibleGone
import kotlinx.android.synthetic.main.fragment_item.*

class ItemFragment(
    private val varIds: List<Long> = mutableListOf() // TODO save state
) : Fragment() {

    private var fieldData: Data = mutableMapOf()
    private val dao = App.db.variableDao()
    private val items = mutableListOf<Variable>()
    private val adapter = MainFieldAdapter(
        variables = items,
        data = fieldData,
        // changeListener = { onDataUpdated() },
        onEdit = { navigateToEditVariable(it) }
    )

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
        list_fields?.adapter = adapter

        btn_add?.setOnClickListener {
            (activity?.supportFragmentManager?.getCallerFragment() as MainFragment?)
                ?.addItem(fieldData)
            activity?.supportFragmentManager?.popBackStack()
        }

        dao.getByIdsLiveData(varIds).observe(viewLifecycleOwner) {
            items.clear()
            items.addAll(it)
            adapter.notifyDataSetChanged()
            text_empty?.manageVisibleGone(it.isEmpty())
        }
    }

    private fun navigateToEditVariable(variable: Variable) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
            ?.replace(R.id.layout_activity_container, FieldFragment(false, variable))
            ?.addToBackStack(null)?.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.btn_menu_close) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

}