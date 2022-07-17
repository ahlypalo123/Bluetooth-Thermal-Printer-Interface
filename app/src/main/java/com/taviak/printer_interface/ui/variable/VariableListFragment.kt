package com.taviak.printer_interface.ui.variable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableScope
import com.taviak.printer_interface.ui.template.TemplateEditorFragment
import com.taviak.printer_interface.util.*
import kotlinx.android.synthetic.main.fragment_variable_list.*
import kotlinx.android.synthetic.main.item_variable.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VariableListFragment(private val forItem: Boolean) : Fragment() {

    private val dao: VariableDao = App.db.variableDao()
    private val itemVariables = mutableListOf<Variable>()
    private val itemVarAdapter = Adapter(itemVariables)
    private val commonVariables = mutableListOf<Variable>()
    private val commonVarAdapter = Adapter(commonVariables)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_variable_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        list_variables?.layoutManager = LinearLayoutManager(context)
        list_variables?.adapter = commonVarAdapter
        list_variables_list?.layoutManager = LinearLayoutManager(context)
        list_variables_list?.adapter = itemVarAdapter

        btn_create?.setOnClickListener {
            VariableDialog(forItem, null).show(activity?.supportFragmentManager!!, null)
        }

        if (forItem) {
            dao.getAllLiveData()
        } else {
            dao.getAllByScopeLiveData(VariableScope.COMMON.ordinal)
        }.observe(viewLifecycleOwner) {
            updateUi(it)
        }
    }

    private fun updateUi(list: List<Variable>) {
        commonVariables.clear()
        itemVariables.clear()

        if (forItem) {
            list.forEach { variable ->
                if (variable.scope == VariableScope.COMMON.ordinal) {
                    commonVariables.add(variable)
                } else {
                    itemVariables.add(variable)
                }
            }
            layout_list_variables?.manageVisibleGone(itemVariables.isNotEmpty())
            layout_common_variables?.manageVisibleGone(itemVariables.isNotEmpty())
            text_empty_variables?.manageVisibleGone(itemVariables.isEmpty() && list.isEmpty())
        } else {
            commonVariables.addAll(list)
            layout_list_variables?.gone()
            layout_common_variables?.gone()
            text_empty_variables?.manageVisibleGone(list.isEmpty())
        }
        commonVarAdapter.notifyDataSetChanged()
        itemVarAdapter.notifyDataSetChanged()
    }

    inner class Adapter(
        private val list: List<Variable>
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_variable))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: Variable) = with(itemView) {
                text_variable_name?.text = "${item.shortName}:${item.name}"
                btn_delete?.setOnClickListener {
                    activity?.confirm("Вы действительно хотите удалить переменную?", onYes = {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.delete(item)
                        }
                    })
                }
                btn_edit?.setOnClickListener {
                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
                        ?.replace(R.id.layout_activity_container, FieldFragment(forItem, variable = item))
                        ?.addToBackStack(null)?.commit()
                }
                setOnClickListener {
                    activity?.supportFragmentManager?.popBackStack()
                    val caller = activity?.supportFragmentManager?.getCallerFragment()
                    if (caller is TemplateEditorFragment) {
                        caller.onVariableSelected(item.shortName!!)
                    }
                }
            }
        }
    }

}