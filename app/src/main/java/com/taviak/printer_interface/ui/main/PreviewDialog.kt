package com.taviak.printer_interface.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.Receipt
import com.taviak.printer_interface.util.ReceiptBuilder
import com.taviak.printer_interface.util.setMaxHeight
import kotlinx.android.synthetic.main.dialog_preview.*

class PreviewDialog(
    private val receipt: Receipt
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return inflater.inflate(R.layout.dialog_preview, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            dialog?.dismiss()
        }
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val image = ReceiptBuilder(context, receipt, false).build(receipt.templateData)
        image_receipt?.setImageBitmap(image)
        btn_ok?.setOnClickListener {
            dismiss()
        }
    }
}