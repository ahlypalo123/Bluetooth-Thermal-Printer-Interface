package com.taviak.printer_interface.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.PREF_HEIGHT
import com.taviak.printer_interface.data.PREF_PRINTER_ADDRESS
import com.taviak.printer_interface.data.model.*
import com.taviak.printer_interface.ui.devices.DevicesFragment
import com.taviak.printer_interface.ui.template.TemplateEditorFragment
import com.taviak.printer_interface.ui.template.TemplateListFragment
import com.taviak.printer_interface.ui.variable.FieldFragment
import com.taviak.printer_interface.util.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.list_fields
import kotlinx.android.synthetic.main.item_list.view.*
import kotlinx.android.synthetic.main.item_list_item.view.*
import kotlinx.android.synthetic.main.item_select_picture.view.*
import java.util.concurrent.Executors

class MainFragment : Fragment(), MainView, MainScriptView {

    override var listData: ListData = mutableMapOf()
    override var fieldData: Data = mutableMapOf()

    private var pictureData: Data = mutableMapOf() // key - field name, value - file name

    private val listsAdapter = ListAdapter()
    private val pictureAdapter = PictureAdapter()
    private val executor = Executors.newSingleThreadExecutor()
    private val presenter: MainPresenter = MainPresenter(this, this)
    private val variablesAdapter: MainFieldAdapter = MainFieldAdapter(
        variables = presenter.variables,
        data = fieldData,
        changeListener = { onDataUpdated() },
        onEdit = { navigateToEditVariable(it) }
    )
    private val scriptPresenter = MainScriptPresenter(
        variables = presenter.variables, view = this)

    private var updatedListKey: String? = null
    private var updatedPictureKey: String? = null

    private val requestGetContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { result ->
        result ?: return@registerForActivityResult
        executor.submit {
            pictureData[updatedPictureKey!!] = saveImageForReceipt(requireContext(), result)
            view?.post {
                pictureAdapter.notifyDataSetChanged()
                updateReceipt()
            }
        }
    }

    private val requestStorePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestGetContent.launch("image/*")
        }
    }

    private val bluetoothRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            print()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("listData", Gson().toJson(listData))
        outState.putString("fieldData", Gson().toJson(fieldData))
        outState.putString("pictureData", Gson().toJson(pictureData))
        outState.putString("updatedListKey", updatedListKey)
        outState.putString("updatedPictureKey", updatedPictureKey)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            listData = Gson().fromJson(
                it.getString("listData"),
                (object : TypeToken<ListData>() {}).type
            )
            fieldData = Gson().fromJson(
                it.getString("fieldData"),
                (object : TypeToken<Data>() {}).type
            )
            pictureData = Gson().fromJson(
                it.getString("pictureData"),
                (object : TypeToken<Data>() {}).type
            )
            updatedListKey = it.getString("updatedListKey")
            updatedPictureKey = it.getString("updatedPictureKey")
        }
        super.onCreate(savedInstanceState)
    }

    private fun navigateToEditVariable(variable: Variable) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
            ?.replace(R.id.layout_activity_container, FieldFragment(false, variable))
            ?.addToBackStack(null)?.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list_fields?.recycledViewPool?.clear()
        list_fields?.layoutManager = NpaLinearLayoutManager(context)
        list_fields?.adapter = variablesAdapter

        list_of_list?.layoutManager = NpaLinearLayoutManager(context)
        list_of_list?.adapter = listsAdapter

        list_pictures?.layoutManager = NpaLinearLayoutManager(context)
        list_pictures?.adapter = pictureAdapter

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

        btn_edit_template?.setOnClickListener {
            editActiveTemplate()
        }

        presenter.init()
        scriptPresenter.init(context)
    }

    private fun editActiveTemplate() {
        presenter.template ?: return
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out,
                R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container,
                TemplateEditorFragment(presenter.template!!), TemplateEditorFragment::class.simpleName)
            ?.addToBackStack(TemplateEditorFragment::class.simpleName)?.commit()
    }

    fun print() {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            bluetoothRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        val receipt = Receipt(
            fieldData = fieldData,
            listData = listData,
            templateData = presenter.template?.data,
            pictureData = pictureData
        )
        startPrinting(receipt)
    }

    private fun navigateToPrinters() {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out, R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container, DevicesFragment())
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

    private fun onDataUpdated() {
        updateReceipt()
        // scriptPresenter.onVariablesUpdated()
    }

    private fun updateReceipt() {
        val receipt = Receipt(
            fieldData = fieldData,
            listData = listData,
            templateData = presenter.template?.data,
            pictureData = pictureData
        )
        val image = ReceiptBuilder(context, receipt, false).build(receipt.templateData)
        image_receipt?.setImageBitmap(image)
    }

    fun addItem(item: Data) {
        if (!listData.containsKey(updatedListKey)) {
            listData[updatedListKey!!] = mutableListOf()
        }
        listData[updatedListKey]?.add(item)
        listsAdapter.notifyDataSetChanged()
        onDataUpdated()
    }

    override fun onExpressionEvaluated() {
        view?.post {
            variablesAdapter.disableListeners = true
            variablesAdapter.notifyDataSetChanged()
        }
    }

    inner class ListAdapter : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_list))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(presenter.lists[position])

        override fun getItemCount() = presenter.lists.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(el: ReceiptListElement) = with(itemView) {
                list_items?.recycledViewPool?.clear()
                list_items?.layoutManager = NpaLinearLayoutManager(context)
                list_items?.adapter = ListItemAdapter(listData[el.name] ?: mutableListOf())
                text_list_name?.text = el.name
                btn_add_item?.setOnClickListener {
                    updatedListKey = el.name
                    presenter.onAddItem(el)
                }
                return@with
            }
        }
    }

    inner class ListItemAdapter(
        private val list: MutableList<Data>
    ) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_list_item))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(list[position])

        override fun getItemCount() = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(item: Data) = with(itemView) {
                val params = item.entries.map { "${it.key}: ${it.value}" }
                btn_remove?.setOnClickListener {
                    list.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    onDataUpdated()
                }
                list_params?.adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_text,
                    params
                )
                return@with
            }
        }
    }

    inner class PictureAdapter : RecyclerView.Adapter<PictureAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(parent.inflate(R.layout.item_select_picture))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(presenter.pictures[position])

        override fun getItemCount() = presenter.pictures.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(el: ReceiptImageElement) = with(itemView) {
                val fileName = pictureData[el.name]
                if (fileName == null) {
                    image_picture?.setImageResource(R.drawable.ic_picture)
                } else {
                    image_picture?.setImage(context, fileName)
                }
                text_field_name?.text = el.name
                layout_picture?.setOnClickListener {
                    updatedPictureKey = el.name
                    requestStorePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                return@with
            }
        }
    }

    override fun onFieldsUpdated() {
        list_fields?.recycledViewPool?.clear()
        variablesAdapter.notifyDataSetChanged()
        listsAdapter.notifyDataSetChanged()
        pictureAdapter.notifyDataSetChanged()
        updateReceipt()
    }

    override fun navigateToItem(data: List<Variable>) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_bottom, R.anim.fade_out,
                R.anim.fade_in, R.anim.slide_to_bottom)
            ?.replace(R.id.layout_activity_container, ItemFragment(data.map { it.id }))
            ?.addToBackStack(null)?.commit()
    }

}