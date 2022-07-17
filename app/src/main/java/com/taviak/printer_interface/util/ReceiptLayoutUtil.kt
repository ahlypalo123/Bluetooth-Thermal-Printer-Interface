package com.taviak.printer_interface.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.ReceiptImageElement
import com.taviak.printer_interface.data.model.ReceiptTextElement
import com.taviak.printer_interface.data.model.ReceiptTextStyle
import kotlinx.android.synthetic.main.layout_image_element.view.*
import kotlinx.android.synthetic.main.layout_image_element_preview.view.*
import kotlinx.android.synthetic.main.layout_image_element_preview.view.layout_image
import java.io.File
import java.util.*

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
    view?.layoutParams = if (view?.layoutParams == null) {
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1F
        }
    } else {
        view.layoutParams as LinearLayout.LayoutParams
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

@SuppressLint("NewApi")
fun createBlackAndWhite(src: Bitmap) : Bitmap {
    val copy = src.copy(Bitmap.Config.RGBA_F16, true)
    val bmOut = Bitmap.createBitmap(copy.width, copy.height, Bitmap.Config.ARGB_8888)
    // scan through all pixels
    for (x in 0 until copy.width) {
        for (y in 0 until copy.height) {
            // get pixel color
            val pixel = copy.getPixel(x, y)
            val R = Color.red(pixel)
            val G = Color.green(pixel)
            val B = Color.blue(pixel)
            val scale: Float = (0.2989F * R + 0.5870F * G + 0.1140F * B)
            val gray = if (scale > 128) 255 else 0

            bmOut.setPixel(x, y, Color.rgb(gray, gray, gray))
        }
    }
    return bmOut
}

fun ImageView?.setImage(context: Context?, fileName: String?) {
    this ?: return
    val uri = File(
        context?.getExternalFilesDir("."),
        fileName!!
    ).toUri()
    setImageURI(uri)
}

fun getViewForImageElementPreview(el: ReceiptImageElement, context: Context?) : View {
    val view = View.inflate(context, R.layout.layout_image_element_preview, null)
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

fun getViewForImageElementCompleted(el: ReceiptImageElement, context: Context?, fileName: String) : View {
    val view = View.inflate(context, R.layout.layout_image_element, null)
    with(view) {
        (layout_image?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
            matchConstraintPercentWidth = el.width
            horizontalBias = el.offset
            layout_image.layoutParams = this
        }
        layout_image?.setImage(context, fileName)
    }
    return view
}

fun saveImageForReceipt(context: Context, uri: Uri) : String {
    // TODO progress bar
    val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    val bitmap = createBlackAndWhite(src)
    val name = "picture-${UUID.randomUUID()}.png"
    File(
        context.getExternalFilesDir("."),
        name
    ).outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    return name
}