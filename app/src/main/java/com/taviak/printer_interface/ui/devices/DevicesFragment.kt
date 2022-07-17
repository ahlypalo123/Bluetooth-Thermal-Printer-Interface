package com.taviak.printer_interface.ui.devices

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.taviak.printer_interface.App
import com.taviak.printer_interface.R
import com.taviak.printer_interface.data.PREF_PRINTER_ADDRESS
import com.taviak.printer_interface.data.PREF_PRINTER_NAME
import com.taviak.printer_interface.data.model.Receipt
import com.taviak.printer_interface.ui.main.MainFragment
import com.taviak.printer_interface.util.getCallerFragment
import kotlinx.android.synthetic.main.fragment_printers.*

class DevicesFragment : BottomSheetDialogFragment() {

    private lateinit var listAdapter: ArrayAdapter<String>
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val requestEnableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateList()
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_printers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        activity?.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_hamburger)
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        listAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1)
        paired_devices.adapter = listAdapter
        paired_devices.setOnItemClickListener { _, tv, _, _ ->
            try {
                val mDeviceInfo = (tv as TextView).text.toString()
                val address = mDeviceInfo.substring(mDeviceInfo.length - 17)
                val name = mDeviceInfo.substring(0, mDeviceInfo.length - 17)
                App.prefEditor.putString(PREF_PRINTER_ADDRESS, address).commit()
                App.prefEditor.putString(PREF_PRINTER_NAME, name).commit()
                updateStatus()
                Toast.makeText(context, "Connected Successfully", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Log.e("TAG", ex.localizedMessage, ex)
            }
        }

        if (bluetoothAdapter.isEnabled) {
            updateList()
        } else {
            requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        updateStatus()

        btn_test?.setOnClickListener {
            val caller = activity?.supportFragmentManager?.getCallerFragment()
            (caller as MainFragment).print()
            dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_devices, menu)
    }

    private fun getTestReceipt() : Receipt {
        return Receipt()
    }

    private fun updateList() {
//        val permission = ActivityCompat.checkSelfPermission(
//            requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            activity?.alert("Permission denied")
//            activity?.supportFragmentManager?.popBackStack()
//            return
//        }
        val devices = bluetoothAdapter.bondedDevices

        listAdapter.clear()

        if (devices.size > 0) {
            text_empty?.visibility = View.GONE
            for (d in devices) {
                listAdapter.add("${d.name}\n${d.address}")
            }
        } else {
            text_empty?.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refresh) {
            updateList()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateStatus() {
        val last = App.sharedPrefs.getString(PREF_PRINTER_NAME, "")
        if (last.isNullOrBlank()) {
            text_connected?.text = "Выберите устройство из списка для подключения"
        } else {
            text_connected?.text = "Подключенное устройство: $last"
        }
    }

}