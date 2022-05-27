package com.taviak.printer_interface.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.taviak.printer_interface.R
import com.taviak.printer_interface.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.layout_activity_container, MainFragment(), MainFragment::class.simpleName)
            .addToBackStack(MainFragment::class.simpleName).commit()
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }
}