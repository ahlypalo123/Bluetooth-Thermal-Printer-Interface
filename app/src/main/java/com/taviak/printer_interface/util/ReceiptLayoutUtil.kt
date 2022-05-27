package com.taviak.printer_interface.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Insets
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.ReceiptTextElement
import com.taviak.printer_interface.data.model.ReceiptTextStyle

fun getViewForTextElement(el: ReceiptTextElement, context: Context?) : TextView {
    val tv = TextView(context)
    tv.textAlignment = el.alignment
    tv.setTextColor(ContextCompat.getColor(context!!, R.color.black))

    val span = SpannableStringBuilder(el.text)
    el.style.forEach {
        when (it) {
            ReceiptTextStyle.BOLD -> {
                span.setSpan(StyleSpan(Typeface.BOLD), 0, span.length, 0)
            }
            ReceiptTextStyle.UNDERLINED -> {
                span.setSpan(UnderlineSpan(), 0, span.length, 0)
            }
            ReceiptTextStyle.ITALIC -> {
                span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, 0)
            }
        }
    }
    tv.text = span
    tv.textSize = 26F + (el.size.offset)
    return tv
}

fun createRow(context: Context?, parent: LinearLayout) : LinearLayout {
    val tr = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    tr.orientation = LinearLayout.HORIZONTAL
    parent.addView(tr)
    return tr
}

fun addViewToRow(view: View?, row: LinearLayout) {
    view?.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        weight = 1F
    }
    row.addView(view)
}

fun getPreviewLayoutWidth(context: Context?) : Int {
    val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics: WindowMetrics =
            (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics
        val windowInsets = metrics.windowInsets
        val insets: Insets = windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.navigationBars()
                    or WindowInsets.Type.displayCutout()
        )
        val insetsWidth: Int = insets.left + insets.right
        val bounds: Rect = metrics.bounds

        bounds.width() - insetsWidth
    } else {
        val metrics = Resources.getSystem().displayMetrics
        metrics.widthPixels
    }
    return (width - 32.toPx).toInt()
}