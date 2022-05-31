package com.taviak.printer_interface.ui.template

import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.percentlayout.widget.PercentFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.*
import com.taviak.printer_interface.util.*
import kotlinx.android.synthetic.main.dialog_image_format.*
import kotlinx.android.synthetic.main.fragment_edit_template.*
import kotlinx.android.synthetic.main.fragment_edit_template.btn_delete
import kotlinx.android.synthetic.main.layout_image_element.*
import kotlinx.android.synthetic.main.layout_image_element.view.*

class TemplateEditorFragment(
    private var data: ReceiptTemplateData = mutableListOf()
) : Fragment() {

    private var draggingElement: ReceiptElement? = null
    private var targetRow: Int = -1
    private var targetCol: Int = -1
    private var variableAdded: Boolean = false
    private var targetIsList: Boolean = false
    private var targetList: ReceiptListElement? = null
    private var targetListRow: Int = -1
    private var targetListCol: Int = -1
    private var modifyData: ReceiptTemplateData = data
    private var updated: Boolean = false

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("data", Gson().toJson(data))
        outState.putInt("targetElementRow", targetRow)
        outState.putInt("targetElementCol", targetCol)
        outState.putBoolean("variableAdded", variableAdded)
        outState.putBoolean("targetIsList", targetIsList)
        outState.putInt("targetListRow", targetListRow)
        outState.putInt("targetListCol", targetListCol)
        outState.putBoolean("updated", updated)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            data = Gson().fromJson(
                it.getString("data"),
                (object : TypeToken<ReceiptTemplateData>() {}).type
            )
            targetRow = it.getInt("targetElementRow")
            targetCol = it.getInt("targetElementCol")
            variableAdded = it.getBoolean("variableAdded")
            targetIsList = it.getBoolean("targetIsList")
            targetListRow = it.getInt("targetListRow")
            targetListCol = it.getInt("targetListCol")
            updated = it.getBoolean("updated")
            if (targetIsList) {
                targetList = data[targetListRow][targetListCol] as ReceiptListElement
                modifyData = targetList!!.data
            } else {
                modifyData = data
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_template, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_editor?.layoutParams?.width = getPreviewLayoutWidth(context)

        layout_editor?.setOnDragListener { _, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    layout_editor?.setBackgroundResource(
                        R.drawable.shape_dashed_lines_highlighted
                    )
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    layout_editor?.setBackgroundResource(
                        R.drawable.shape_dashed_lines
                    )
                    true
                }
                DragEvent.ACTION_DROP -> {
                    layout_editor?.setBackgroundResource(
                        R.drawable.shape_dashed_lines
                    )

                    data.add(mutableListOf(draggingElement!!))
                    updateUi()

                    true
                }
                else -> {
                    false
                }
            }
        }

        btn_add_text?.setOnClickListener {
            val el = ReceiptTextElement(text = "Text")
            modifyData.add(mutableListOf(el))
            updateElement(col = 0, row = modifyData.size - 1)
            updateUi()
        }

        btn_add_variable?.setOnClickListener {
            val el = ReceiptTextElement(text = "")
            modifyData.add(mutableListOf(el))
            targetCol = 0
            targetRow = modifyData.size - 1
            openAddVariableDialog()
        }

        btn_add_image?.setOnClickListener {
            val el = ReceiptImageElement(
                path = "",
                name = getDefaultElementName("Картинка"),
                width = 1F
            )
            modifyData.add(mutableListOf(el))
            updateElement(row = modifyData.size - 1, col = 0)
            updateUi()
        }

        btn_add_list?.setOnClickListener {
            addElementGroup(ReceiptElementGroupType.LIST)
        }

        btn_add_group?.setOnClickListener {
            addElementGroup(ReceiptElementGroupType.GROUP)
        }

        btn_done?.setOnClickListener {
            updated = false
            if (targetIsList) {
                clearTargetList()
                updateUi()
                return@setOnClickListener
            }
            val caller = activity?.supportFragmentManager?.getCallerFragment()
            if (caller is TemplateEditorFragment) {
                caller.updateUi()
            }
            if (caller is TemplateListFragment) {
                caller.saveTemplate(data)
            }
            Snackbar.make(
                layout_edit_template,
                "Изменения успешно сохранены",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        btn_delete?.setOnClickListener {
            val caller = activity?.supportFragmentManager?.getCallerFragment()
            if (caller is TemplateListFragment) {
                activity.confirm("Вы действительно хотите удалить шаблон?", onYes = {
                    caller.removeTemplate()
                    parentFragmentManager.popBackStack()
                })
            }
        }

        if (variableAdded) {
            variableAdded = false
            updateElement(col = targetCol, row = targetRow)
        }

        btn_list_delete?.setOnClickListener {
            activity.confirm("Вы действительно хотите удалить группу?", onYes = {
                data[targetListRow].removeAt(targetListCol)
                if (data[targetListRow].isEmpty()) {
                    data.removeAt(targetListRow)
                }
                clearTargetList()
            })
        }

        btn_list_done?.setOnClickListener {
            clearTargetList()
        }

        btn_cancel?.setOnClickListener {
            if (updated) {
                activity.confirm(
                    "Несохраненные изменения удалятся. Вы действительно хотите выйти?",
                    onYes = {
                        activity?.onBackPressed()
                    })
            } else {
                activity?.onBackPressed()
            }
        }

        btn_list_edit_name?.setOnClickListener {
            ListNameDialog(targetList!!.name).show(childFragmentManager, null)
        }

        scroll_view_editor?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (targetIsList) {
                val diff = scrollY - oldScrollY
                (layout_list_action_buttons.layoutParams as RelativeLayout.LayoutParams?)?.let { lp ->
                    lp.setMargins(lp.leftMargin, lp.topMargin - diff, 0, 0)
                    layout_list_action_buttons.layoutParams = lp
                }
            }
        }
        updateUi()
    }

    private fun addElementGroup(type: ReceiptElementGroupType) {
        if (targetIsList) {
            Toast.makeText(context, "Извините, вы не можете добавить группу в группу", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val prefix = if (type == ReceiptElementGroupType.LIST) "Список" else "Группа"
        val el = ReceiptListElement(
            data = mutableListOf(),
            name = getDefaultElementName(prefix),
            groupType = type
        )
        modifyData.add(mutableListOf(el))
        updateElement(col = 0, row = modifyData.size - 1)
        updateUi()
    }

    private fun startDragging(v: View) {
        val maskShadow = DragShadowBuilder(v)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            v.startDrag(null, maskShadow, v, 0)
        } else {
            v.startDragAndDrop(null, maskShadow, v, 0)
        }
    }

    private fun clearTargetList() {
        targetIsList = false
        modifyData = data
        targetList = null
        targetListRow = -1
        targetListCol = -1
        updateUi()
    }

    private fun getDefaultElementName(prefix: String) : String {
        var cur = 0
        data.flatten().forEach {
            if (it is ReceiptListElement && Regex("$prefix \\d").matches(it.name)) {
                val num = it.name.split(" ")[1].toIntOrNull() ?: 0
                if (num > cur) {
                    cur = num
                }
            }
        }
        return "$prefix ${cur + 1}"
    }

    fun openAddVariableDialog() {
        variableAdded = true
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.pop_enter, R.anim.pop_exit)
            ?.replace(R.id.layout_activity_container, VariableListFragment(forItem = targetIsList))
            ?.addToBackStack(null)?.commit()
    }

    fun updateUi() {
        updated = true
        layout_editor?.removeAllViews()

        layout_list_action_buttons?.invisible()

        data.forEachIndexed { rowInd, row ->
            val tr = createRow(context, layout_editor)
            row.forEachIndexed { colInd, el ->
                val v = getElementView(el)
                addViewToRow(v, tr)
                if (!targetIsList) {
                    addDragListener(v, rowInd, colInd)
                }
                val isTargetList = targetIsList && targetList == el
                if (isTargetList) {
                    adjustActionButtonsLayout(v, layout_list_action_buttons)
                } else {
                v?.setOnClickListener {
                    if (targetIsList) {
                        clearTargetList()
                    } else {
                        updateElement(rowInd, colInd)
                    }
                }
            }
                v?.alpha = if (!targetIsList || isTargetList) 1F else 0.3F
            }
        }
    }

    private fun adjustActionButtonsLayout(v: View?, layout: LinearLayout?) = v?.post {
        layout?.post {
            val location = IntArray(2)
            v.getLocationOnScreen(location)
            val top = location[1] - layout.measuredHeight * 1.5F
            (layout.layoutParams as RelativeLayout.LayoutParams?)?.let {
                it.topMargin = top.toInt()
                layout.layoutParams = it
            }
            layout.visible()
        }
    }

    fun onVariableSelected(variable: String) {
        val el = modifyData[targetRow][targetCol] as ReceiptTextElement
        el.text += "{$variable}"
    }

    fun removeElement() {
        deleteElementByPosition(targetRow, targetCol)
    }

    private fun getElementView(el: ReceiptElement) : View? {
        return when(el) {
            is ReceiptTextElement -> getViewForTextElement(el, context)
            is ReceiptListElement -> getViewForListElement(el)
            is ReceiptImageElement -> getViewForImageElement(el)
            else -> null
        }
    }

    private fun getViewForImageElement(el: ReceiptImageElement) : View {
        val view = View.inflate(context, R.layout.layout_image_element, null)
        with(view) {
            text_width?.text = "%.1f".format(el.width)
            (layout_image.layoutParams as ConstraintLayout.LayoutParams).apply {
                matchConstraintPercentWidth = el.width
                horizontalBias = el.offset
                layout_image.layoutParams = this
            }
        }
        return view
    }

    private fun getViewForListElement(parent: ReceiptListElement) : View {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_dashed_lines)
        parent.data.forEachIndexed { rowInd, row ->
            val tr = createRow(context, layout)
            row.forEachIndexed { colInd, child ->
                val v = getElementView(child)
                addViewToRow(v, tr)
                if (targetIsList && targetList == parent) {
                    addDragListener(v, rowInd, colInd)
                    v?.setOnClickListener {
                        updateElement(rowInd, colInd)
                    }
                }
            }
        }
        layout.minimumHeight = if (parent.data.isEmpty()) 24.toPx.toInt() else 0
        return layout
    }

    private fun updateElement(row: Int, col: Int) {
        val el = modifyData[row][col]
        targetCol = col
        targetRow = row
        when (el) {
            is ReceiptTextElement -> {
                TextFormatDialog(el).show(childFragmentManager, null)
                // scrollToTargetElement()
            }
            is ReceiptListElement -> {
                targetIsList = true
                targetList = el
                modifyData = el.data
                targetListRow = row
                targetListCol = col
                updateUi()
            }
            is ReceiptImageElement -> {
                ImageFormatDialog(el).show(childFragmentManager, null)
            }
        }
    }

    private fun scrollToTargetElement() {
        val view = if (targetIsList) {
            val listView = getChildChild(layout_editor, targetListRow, targetListCol)
            getChildChild(listView as ViewGroup?, targetRow, targetCol)
        } else {
            getChildChild(layout_editor as ViewGroup?, targetRow, targetCol)
        }
        val location = IntArray(2)
        view?.getLocationOnScreen(location)
        scroll_view_editor?.smoothScrollTo(0, location[1])
    }

    private fun getChildChild(parent: ViewGroup?, row: Int, col: Int) : View? =
        (parent?.getChildAt(row) as ViewGroup?)?.getChildAt(col)

    private fun deleteElementByPosition(row: Int, col: Int) {
        modifyData[row].removeAt(col)
        if (modifyData[row].isEmpty()) {
            modifyData.removeAt(row)
        }
        updateUi()
    }

    private fun addDragListener(view: View?, row: Int, col: Int) {
        view ?: return
        var position = Position.BOTTOM

        view.setOnLongClickListener {
            draggingElement = modifyData[row][col]
            startDragging(view)
            deleteElementByPosition(row, col)
            true
        }

        view.setOnDragListener { v, e ->
            val parent = view.parent as? LinearLayout
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    layout_editor?.setBackgroundResource(
                        R.drawable.shape_dashed_lines_highlighted
                    )
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    layout_editor?.setBackgroundResource(
                        R.drawable.shape_dashed_lines
                    )
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    parent?.setBackgroundResource(0)
                    v.setBackgroundResource(0)

                    val part = view.width / 100 * 20
                    when {
                        e.x < part -> {
                            position = Position.LEFT
                            view.setBackgroundResource(R.drawable.shape_border_left)
                        }
                        e.x > (view.width - part) -> {
                            position = Position.RIGHT
                            view.setBackgroundResource(R.drawable.shape_border_right)
                        }
                        e.y > view.height / 2 -> {
                            position = Position.BOTTOM
                            parent?.setBackgroundResource(R.drawable.shape_border_bottom)
                        }
                        else -> {
                            position = Position.TOP
                            parent?.setBackgroundResource(R.drawable.shape_border_top)
                        }
                    }
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    parent?.setBackgroundResource(0)
                    v.setBackgroundResource(0)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    parent?.setBackgroundResource(0)
                    v.setBackgroundResource(0)

                    when (position) {
                        Position.LEFT -> {
                            modifyData[row].add(col, draggingElement!!)
                        }
                        Position.RIGHT -> {
                            modifyData[row].add(col + 1, draggingElement!!)
                        }
                        Position.TOP -> {
                            modifyData.add(row, mutableListOf(draggingElement!!))
                        }
                        Position.BOTTOM -> {
                            modifyData.add(row + 1, mutableListOf(draggingElement!!))
                        }
                    }

                    updateUi()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    fun setListName(name: String) {
        targetList?.name = name
        updateUi()
    }

    private enum class Position {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private class DragShadowBuilder(private val v: View?) : View.DragShadowBuilder(v) {
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            v?.let {
                size.set(it.width, it.height)
                touch.set(it.width / 2, it.height / 2)
            }
        }

        override fun onDrawShadow(canvas: Canvas) {
            v?.draw(canvas)
        }
    }

}