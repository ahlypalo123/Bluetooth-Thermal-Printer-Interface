package com.taviak.printer_interface.ui.main

import com.taviak.printer_interface.data.model.Variable

interface MainView {

    fun onFieldsUpdated()
    fun navigateToItem(data: List<Variable>)

}