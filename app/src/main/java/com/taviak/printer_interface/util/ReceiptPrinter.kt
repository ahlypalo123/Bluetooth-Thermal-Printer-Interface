package com.taviak.printer_interface.util

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.taviak.printer_interface.App
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.model.Receipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.*

class ReceiptPrinter(private val context: Context?, private val address: String) {

    companion object {
        private val applicationUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var socket: BluetoothSocket
    // private val merchantRepo: MerchantRepository by lazy { MerchantRepository.instance }
    private var tries = 2
    private val dao: ReceiptTemplateDao by lazy { App.db.receiptTemplateDao() }

    fun init() {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val device = adapter.getRemoteDevice(address)
        socket = device.createRfcommSocketToServiceRecord(applicationUuid)
        adapter.cancelDiscovery()
    }

    fun connect() : Connection? {
        try {
            socket.connect()
        } catch (e: IOException) {
            return if (tries > 0) {
                tries--
                Log.d("TAG", "Connection failed. Trying one more time. Tries left: $tries")
                connect()
            } else {
                throw e
            }
        }
        Log.d("TAG", "device $address successfully connected")
        return Connection(socket.outputStream)
    }

    inner class Connection(private val os: OutputStream) {
        fun print(receipt: Receipt) {
            val pw = BluetoothPrinterWriter(
                context,
                os
            )
            CoroutineScope(Dispatchers.IO).launch {
                val template = dao.getActive()
                withContext(Dispatchers.Main) {
                    val bmp = ReceiptBuilder(context, receipt, true).build(template?.data)
                    pw.writeImage(bmp)
                }
            }
        }

        public fun close() {
            os.close()
        }
    }
}