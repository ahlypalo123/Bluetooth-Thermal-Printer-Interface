package com.taviak.printer_interface.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class NpaLinearLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }

}