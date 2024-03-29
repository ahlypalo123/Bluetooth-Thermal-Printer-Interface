package com.taviak.printer_interface.util

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.taviak.printer_interface.R
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import android.provider.OpenableColumns
import android.provider.MediaStore
import androidx.annotation.RawRes


val Number.toPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics)

val Number.toDp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_PX,
    this.toFloat(),
    Resources.getSystem().displayMetrics)

fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

fun FragmentManager.getCallerFragment(): Fragment? =
    findFragmentByTag(getBackStackEntryAt(backStackEntryCount - 2).name)

fun View?.visible() {
    if (this == null)
        return
    visibility = View.VISIBLE
}

fun View?.invisible() {
    if (this == null)
        return
    visibility = View.INVISIBLE
}

fun View?.gone() {
    if (this == null)
        return
    visibility = View.GONE
}

fun View?.manageVisible(flag: Boolean) {
    if (this == null)
        return
    visibility = if (flag) View.VISIBLE else View.INVISIBLE
}

fun View?.manageVisibleGone(flag: Boolean) {
    if (this == null)
        return
    visibility = if (flag) View.VISIBLE else View.GONE
}

fun Context?.confirm(
    message: Any?,
    onYes: (() -> Unit)? = null,
    onNo: (() -> Unit)? = null,
    notCancelable: Boolean? = null
) {
    if (this == null)
        return
    val builder = AlertDialog.Builder(this, R.style.CommonDialog)
    if (message is Int)
        builder.setMessage(message)
    if (message is String?)
        builder.setMessage(message)
    builder.setPositiveButton(
        "Да"
    ) { dialog, _ -> dialog.dismiss(); onYes?.invoke() }
    if (notCancelable == true) {
        builder.setCancelable(false)
    }

    builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss(); onNo?.invoke() }
    val dialog = builder.create()

    if (notCancelable == true) {
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
    }

    dialog.show()
}

fun Context?.alert(
    message: Any?,
    title: Any? = null,
    onOk: (() -> Unit)? = null
) {
    if (this == null)
        return
    val builder = AlertDialog.Builder(this, R.style.CommonDialog)
    if (message is Int)
        builder.setMessage(message)
    if (message is String?)
        builder.setMessage(message)
    if (title != null) {
        if (title is Int)
            builder.setTitle(title)
        if (title is String)
            builder.setTitle(title)
    }

    builder.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss(); onOk?.invoke() }
    builder.create().show()
}

fun Context?.readRawContent(@RawRes rawRes: Int) : String? {
    return this?.resources
        ?.openRawResource(rawRes)
        ?.bufferedReader()
        .use { it?.readText() }
}