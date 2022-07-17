package com.taviak.printer_interface.ui.main

import com.taviak.printer_interface.data.model.Data
import com.taviak.printer_interface.data.model.ListData
import com.taviak.printer_interface.data.model.Variable

interface MainScriptView {

    val listData: ListData
    val fieldData: Data
    fun onExpressionEvaluated();

}