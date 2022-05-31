package com.taviak.printer_interface.ui.main

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.PREF_HEIGHT
import com.taviak.printer_interface.data.PREF_PRINTER_ADDRESS
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.*
import com.taviak.printer_interface.ui.template.TemplateListFragment
import com.taviak.printer_interface.util.*
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
import java.util.concurrent.Executors
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class MainFragment : Fragment() {

    private var variables: MutableList<Variable> = mutableListOf()
    private var lists: MutableList<ReceiptListElement> = mutableListOf()
    private var listData: ListData = mutableMapOf()
    private var fieldData: MutableMap<String, String?> = mutableMapOf()

    private var variablesAdapter = FieldAdapter(variables, fieldData) {
        updateReceipt()
    }
    private val listsAdapter = ListAdapter()
    private val variableDao: VariableDao = App.db.variableDao()

    private val dao: ReceiptTemplateDao = App.db.receiptTemplateDao()
    private var updatedListIndex = -1

    private var templateData: ReceiptTemplateData? = null

    private val bluetoothRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            print()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("variables", Gson().toJson(variables))
        outState.putString("lists", Gson().toJson(lists))
        outState.putString("listData", Gson().toJson(listData))
        outState.putString("fieldData", Gson().toJson(fieldData))
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
            listsAdapter.notifyDataSetChanged()
        }
        super.onCreate(savedInstanceState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            fieldData = Gson().fromJson(
                it.getString("fieldData"),
                (object : TypeToken<MutableMap<String, String?>>() {}).type
            )
            variablesAdapter = FieldAdapter(variables, fieldData)
        }
        super.onViewStateRestored(savedInstanceState)
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

        (scroll_view_preview?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
            it.height = App.sharedPrefs.getInt(PREF_HEIGHT, 100.toPx.toInt())
            scroll_view_preview?.layoutParams = it
        }

        bottom_appbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.btn_menu_settings -> {
                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
                        ?.replace(R.id.layout_activity_container, TemplateListFragment(), TemplateListFragment::class.simpleName)
                        ?.addToBackStack(TemplateListFragment::class.simpleName)?.commit()
                }
            }
            true
        }

        var downRawY = 0F
        view_scale?.setOnTouchListener { v, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    downRawY  = motionEvent.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawY = motionEvent.rawY
                    val max = Resources.getSystem().displayMetrics.heightPixels - 84.toPx
                    if (rawY > max || rawY < 10.toPx) {
                        return@setOnTouchListener true
                    }
                    (scroll_view_preview?.layoutParams as ConstraintLayout.LayoutParams?)?.let {
                        it.height += (rawY - downRawY).toInt()
                        downRawY = motionEvent.rawY
                        scroll_view_preview?.layoutParams = it
                    }
                }
                MotionEvent.ACTION_UP -> {
                    scroll_view_preview?.height?.let {
                        App.prefEditor.putInt(PREF_HEIGHT, it).commit()
                    }
                }
            }
            true
        }

        btn_print?.setOnClickListener {
            val address = App.sharedPrefs.getString(PREF_PRINTER_ADDRESS, "")
            if (address.isNullOrBlank()) {
                navigateToPrinters()
            } else {
                print()
            }
        }

        updateTemplate()
    }

    fun print() {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            bluetoothRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        CoroutineScope(Dispatchers.IO).launch {
            templateData = dao.getActive()?.data
            val receipt = Receipt(
                data = fieldData,
                listData = listData,
                templateData = templateData
            )
            withContext(Dispatchers.Main) {
                startPrinting(receipt)
            }
        }
    }

    private fun navigateToPrinters() {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container, PrintersFragment())
            ?.addToBackStack(null)?.commit()
    }

    private fun startPrinting(receipt: Receipt) = Executors.newSingleThreadExecutor().execute {
        try {
            BluetoothPrinterUtil.printReceipt(receipt, view, context)
        } catch (ex: Exception) {
            ex.printStackTrace()
            view?.post {
                activity?.alert("Не удалось установить соединение с принтером")
            }
        }
    }

    private fun updateReceipt() {
        val receipt = Receipt(
            data = fieldData,
            listData = listData,
            templateData = templateData
        )
        val image = ReceiptBuilder(context, receipt, false).build(receipt.templateData)
        image_receipt?.setImageBitmap(image)
    }

    private fun updateTemplate() {
        CoroutineScope(Dispatchers.IO).launch {
            templateData = dao.getActive()?.data ?: return@launch
            variables.clear()
            lists.clear()
            templateData!!.flatten().forEach { el ->
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
                updateReceipt()
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
        listsAdapter.notifyDataSetChanged()
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
                list_items?.adapter = ListItemAdapter(listData[el.name] ?: mutableListOf())
                text_list_name?.text = el.name
                btn_add_item?.setOnClickListener {
                    updatedListIndex = adapterPosition
                    CoroutineScope(Dispatchers.IO).launch {
                        val itemData = el.data.flatten()
                            .map { extractVariables(it) }.flatten()
                            .filter { it.scope == VariableScope.ITEM.ordinal }
                        withContext(Dispatchers.Main) {
                            activity?.supportFragmentManager
                                ?.beginTransaction()
                                ?.setCustomAnimations(
                                    R.anim.slide_from_bottom,
                                    R.anim.fade_out,
                                    R.anim.fade_in,
                                    R.anim.slide_to_bottom
                                )
                                ?.replace(R.id.layout_activity_container, ItemFragment(itemData))
                                ?.addToBackStack(null)?.commit()
                        }
                    }
                }
                return@with
            }
        }
    }

    inner class ListItemAdapter(
        private val list: MutableList<ReceiptItem>
    ) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_list_item))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: ReceiptItem) = with(itemView) {
                val params = item?.entries?.map { "${it.key}: ${it.value}" }
                btn_remove?.setOnClickListener {
                    list.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    updateReceipt()
                }
                list_params?.adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_text,
                    params ?: listOf()
                )
            }
        }
    }

}