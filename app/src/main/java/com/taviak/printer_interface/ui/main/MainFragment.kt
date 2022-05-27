package com.taviak.printer_interface.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.*
import com.taviak.printer_interface.ui.template.TemplateListFragment
import com.taviak.printer_interface.util.inflate
import kotlinx.android.synthetic.main.fragment_item.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.list_fields
import kotlinx.android.synthetic.main.item_field_edittext.view.*
import kotlinx.android.synthetic.main.item_field_materialbutton.view.*
import kotlinx.android.synthetic.main.item_field_spinner.view.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.android.synthetic.main.item_list.view.*
import kotlinx.android.synthetic.main.item_list_item.*
import kotlinx.android.synthetic.main.item_list_item.view.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class MainFragment : Fragment() {

    private var variables: MutableList<Variable> = mutableListOf()
    private var lists: MutableList<ReceiptListElement> = mutableListOf()
    private var listData: ListData = mutableMapOf()

    private var variablesAdapter = FieldAdapter(variables)
    private val listsAdapter = ListAdapter()
    private val variableDao: VariableDao = App.db.variableDao()

    private val dao: ReceiptTemplateDao = App.db.receiptTemplateDao()
    private var updatedListIndex = -1

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("variables", Gson().toJson(variables))
        outState.putString("lists", Gson().toJson(lists))
        outState.putString("listData", Gson().toJson(listData))
        outState.putInt("updatedListIndex", updatedListIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            variables = Gson().fromJson(
                it.getString("variable"),
                (object : TypeToken<MutableList<Variable>>() {}).type
            )
            lists = Gson().fromJson(
                it.getString("lists"),
                (object : TypeToken<MutableList<ReceiptListElement>>() {}).type
            )
            listData = Gson().fromJson(
                it.getString("listData"),
                (object : TypeToken<ListData>() {}).type
            )
            updatedListIndex = it.getInt("updatedListIndex")
            variablesAdapter = FieldAdapter(variables)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        list_fields?.layoutManager = LinearLayoutManager(context)
        list_fields?.adapter = variablesAdapter

        list_of_list?.layoutManager = LinearLayoutManager(context)
        list_of_list?.adapter = listsAdapter

        bottom_appbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.btn_menu_settings -> {
                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
                        ?.replace(R.id.layout_activity_container, TemplateListFragment(), TemplateListFragment::class.simpleName)
                        ?.addToBackStack(TemplateListFragment::class.simpleName)?.commit()
                }
                R.id.btn_menu_preview -> {
                    val data: MutableMap<String, String> = mutableMapOf()
                    for (i in 0 until list_fields?.size!!) {
                        val value = (list_fields?.findViewHolderForAdapterPosition(i)
                                as FieldAdapter.ViewHolder?)?.getValue()
                        val name = variables[i].shortName!!
                        data[name] = value ?: ""
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        val receipt = Receipt(
                            data = data,
                            listData = listData,
                            templateData = dao.getActive()?.data
                        )
                        withContext(Dispatchers.Main) {
                            PreviewDialog(receipt).show(childFragmentManager, null)
                        }
                    }
                }
            }
            true
        }

        updateUi()
    }

    private fun updateUi() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = dao.getActive()?.data ?: return@launch
            variables.clear()
            lists.clear()
            data.flatten().forEach { el ->
                when (el) {
                    is ReceiptListElement -> {
                        lists.add(el)
                    }
                    is ReceiptTextElement -> {
                        variables.addAll(extractVariables(el))
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Log.i("TAG", "variables: ${Gson().toJson(variables)}")
                variablesAdapter.notifyDataSetChanged()
                listsAdapter.notifyDataSetChanged()
            }
        }
    }

    suspend fun extractVariables(el: ReceiptElement) : Set<Variable> {
        val res = mutableSetOf<Variable>()
        if (el !is ReceiptTextElement) {
            return res
        }

        var startFrom = 0
        while (true) {
            val ind1 = el.text.indexOf('{', startIndex = startFrom)
            if (ind1 == -1) {
                return res
            }
            val ind2 = el.text.indexOf('}', startIndex = startFrom)
            startFrom = ind2 + 1
            val name = el.text.substring(ind1 + 1, ind2)
            variableDao.findByShortName(name)?.let {
                res.add(it)
            }
        }
    }

    private fun evaluateExpression() {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByName("rhino")
        engine.put("a", 5)
        val result: Any = engine.eval("a + 4 - 2")
    }

    fun addItem(item: ReceiptItem) {
        val name = lists[updatedListIndex].name
        if (!listData.containsKey(name)) {
            listData[name] = mutableListOf()
        }
        listData[name]?.add(item)
        updateUi()
    }

    inner class ListAdapter : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_list))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(lists[position])

        override fun getItemCount() = lists.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(el: ReceiptListElement) = with(itemView) {
                list_items?.layoutManager = LinearLayoutManager(context)
                list_items?.adapter = ListItemAdapter(listData[el.name] ?: listOf())
                text_list_name?.text = el.name
                btn_add_item?.setOnClickListener {
                    updatedListIndex = adapterPosition
                    CoroutineScope(Dispatchers.IO).launch {
                        val itemData = el.data.flatten()
                            .map { extractVariables(it) }.flatten()
                            .filter { it.scope == VariableScope.ITEM.ordinal }
                        activity?.supportFragmentManager
                            ?.beginTransaction()
                            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
                            ?.replace(R.id.layout_activity_container, ItemFragment(itemData))
                            ?.addToBackStack(null)?.commit()
                    }
                }
                return@with
            }
        }
    }

    inner class ListItemAdapter(
        private val list: List<ReceiptItem>
    ) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_list_item))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: ReceiptItem) = with(itemView) {
                val params = item?.entries?.map { "${it.key}: ${it.value}" }
                list_params?.adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_text,
                    params ?: listOf()
                )
            }
        }
    }

}