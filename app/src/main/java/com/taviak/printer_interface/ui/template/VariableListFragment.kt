package com.taviak.printer_interface.ui.template

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
import com.taviak.printer_interface.util.*
import kotlinx.android.synthetic.main.fragment_variable_list.*
import kotlinx.android.synthetic.main.fragment_variable_list.btn_create
import kotlinx.android.synthetic.main.fragment_variable_list.toolbar
import kotlinx.android.synthetic.main.item_variable.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VariableListFragment : Fragment() {

    private val dao: VariableDao = App.db.variableDao()
    private val list: MutableList<Variable> = mutableListOf()
    private val adapter: Adapter = Adapter()

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
        list_variables?.adapter = adapter

        btn_create?.setOnClickListener {
            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
                ?.replace(R.id.layout_activity_container, VariableFragment())
                ?.addToBackStack(null)?.commit()
        }

        dao.getAllLiveData().observe(viewLifecycleOwner) {
            list.clear()
            list.addAll(it)
            text_empty_variables?.manageVisibleGone(it.isEmpty())
            adapter.notifyDataSetChanged()
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_variable))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: Variable) = with(itemView) {
                text_variable_name?.text = "${item.shortName}:${item.name}"
                btn_edit?.setOnClickListener {
                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
                        ?.replace(R.id.layout_activity_container, VariableFragment(item))
                        ?.addToBackStack(null)?.commit()
                }
                btn_delete?.setOnClickListener {
                    activity?.confirm("Вы действительно хотите удалить этот элемент?", onYes = {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.delete(item)
                        }
                    })
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