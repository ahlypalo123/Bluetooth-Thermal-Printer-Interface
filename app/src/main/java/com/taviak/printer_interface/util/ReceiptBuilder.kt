package com.taviak.printer_interface.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.*
import kotlinx.android.synthetic.main.item_select_picture.view.*
import kotlinx.android.synthetic.main.layout_image_element.view.*
import kotlinx.android.synthetic.main.layout_image_element_preview.*
import kotlinx.android.synthetic.main.layout_receipt.view.*

class ReceiptBuilder(
    private val context: Context?,
    private val receipt: Receipt?,
    scale: Boolean = true
) {

    private val layoutWidth = if (scale) RECEIPT_WIDTH else getPreviewLayoutWidth(context)
    private val scaleRatio: Float = layoutWidth / getPreviewLayoutWidth(context).toFloat()

    companion object {
        const val RECEIPT_WIDTH = 384
    }

    fun build(template: ReceiptTemplateData?) : Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_receipt, null)

        template?.forEach { row ->
            val tr = createRow(context, view.layout_receipt)
            row.forEach { col ->
                val el = if (col is ReceiptTextElement && receipt != null) {
                    val clonedEl = col.copy()
                    setVariableValue(clonedEl)
                    clonedEl
                } else {
                    col
                }
                val v = getElementView(el)
                addViewToRow(v, tr)
            }
        }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun getElementView(el: ReceiptElement?) : View? {
        return when(el) {
            is ReceiptTextElement -> {
                getViewForTextElement(el, context).apply {
                    textSize = (26F + (el.size.offset)) * scaleRatio
                }
            }
            is ReceiptListElement -> getViewForListElement(el)
            is ReceiptImageElement -> getViewForImageElement(el)
            else -> null
        }
    }

    private fun getViewForImageElement(el: ReceiptImageElement) : View {
        if (el.fileName != null) {
            return getViewForImageElementCompleted(el, context, el.fileName!!)
        }
        if (receipt == null) {
            return getViewForImageElementPreview(el, context)
        }
        val fileName = receipt.pictureData[el.name] ?: return View(context)
        return getViewForImageElementCompleted(el, context, fileName)
    }

    private fun getViewForListElement(el: ReceiptListElement) : View {
        val layout = TableLayout(context)
        layout.isStretchAllColumns = true
        val data = if (receipt == null) {
            listOf(null)
        } else {
            receipt.listData[el.name]
        }
        data?.forEach { item ->
            el.data.forEach { row ->
                val tr = createRow(context, layout)
                row.forEach { col ->
                    val childEl = if (col is ReceiptTextElement && item != null) {
                        val clonedEl = col.copy()
                        setVariableValue(clonedEl, item)
                        clonedEl
                    } else {
                        col
                    }
                    val v = getElementView(childEl)
                    addViewToRow(v, tr)
                }
            }
        }
        return layout
    }

    private fun setVariableValue(el: ReceiptTextElement, item: Data? = null) {
        Log.d("TAG", el.text)
        val ind1 = el.text.indexOf('{')
        if (ind1 == -1) {
            return
        }
        val ind2 = el.text.indexOf('}')
        val name = el.text.substring(ind1 + 1, ind2)
        Log.d("TAG", "name = $name")
        var value = receipt?.fieldData?.get(name)
        if (value == null) {
            value = item?.get(name)
        }
        Log.d("TAG", "value = $value")
        el.text = el.text.replaceRange(ind1..ind2, value ?: "")
        Log.d("TAG", "replaced text = ${el.text}")
    }

}