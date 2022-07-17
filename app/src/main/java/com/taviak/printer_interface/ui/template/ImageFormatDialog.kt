package com.taviak.printer_interface.ui.template

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.model.ImageElementType
import com.taviak.printer_interface.data.model.ReceiptImageElement
import com.taviak.printer_interface.ui.main.MainFragment
import com.taviak.printer_interface.util.manageVisibleGone
import com.taviak.printer_interface.util.saveImageForReceipt
import kotlinx.android.synthetic.main.dialog_image_format.*
import java.util.concurrent.Executors

class ImageFormatDialog(
    private val el: ReceiptImageElement
) : BottomSheetDialogFragment() {

    private val executor = Executors.newSingleThreadExecutor()

    private val requestGetContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { result ->
        result ?: return@registerForActivityResult
        executor.submit {
            el.fileName =
                saveImageForReceipt(requireContext(), result)
            view?.post {
                updateUi()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return inflater.inflate(R.layout.dialog_image_format, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_width_add?.setOnClickListener {
            el.width += .1F
            updateUi()
        }
        btn_width_reduce?.setOnClickListener {
            el.width -= .1F
            updateUi()
        }
        btn_offset_add?.setOnClickListener {
            el.offset += .1F
            updateUi()
        }
        btn_offset_reduce?.setOnClickListener {
            el.offset -= .1F
            updateUi()
        }

        input_value?.setText(el.name)

        input_value?.addTextChangedListener {
            el.name = it.toString()
        }

        btn_ok?.setOnClickListener {
            dismiss()
        }
        btn_delete?.setOnClickListener {
            (parentFragment as TemplateEditorFragment).removeElement()
            dismiss()
        }

        btn_select_picture?.manageVisibleGone(el.imageType == ImageElementType.CONSTANT)
        btn_select_picture?.setOnClickListener {
            requestStorePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        updateValues()
    }

    private fun updateValues() {
        text_size?.text = "Размер ${"%.1f".format(el.width)}"
        text_offset?.text = "Сдвиг ${"%.1f".format(el.offset)}"
        btn_width_add?.isEnabled = el.width < 1
        btn_width_reduce?.isEnabled = el.width > .1
        btn_offset_add?.isEnabled = el.offset < 1
        btn_offset_reduce?.isEnabled = el.offset > .1
    }

    private fun updateUi() {
        updateValues()
        (parentFragment as TemplateEditorFragment).updateUi()
    }

}