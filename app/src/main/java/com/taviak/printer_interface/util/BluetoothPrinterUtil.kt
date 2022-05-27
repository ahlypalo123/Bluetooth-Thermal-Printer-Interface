package com.taviak.printer_interface.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.taviak.printer_interface.App
import com.taviak.printer_interface.data.PREF_PRINTER_ADDRESS
import com.taviak.printer_interface.data.model.Receipt
import java.util.*

object BluetoothPrinterUtil {

    fun printReceipt(
        receipt: Receipt,
        view: View?,
        context: Context?
    ) {
        val address = App.sharedPrefs.getString(PREF_PRINTER_ADDRESS, "")
        if (address.isNullOrBlank()) {
            return
        }
        val printer = CheckPrinter(context, address)
        printer.init()

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                view?.post {
                    Toast.makeText(
                        context,
                        "Пожалуйста подождите, первый раз подключение может занять много времени",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }, 2000L)
        val con = printer.connect()
        timer.cancel()
        con?.print(receipt)
    }

}