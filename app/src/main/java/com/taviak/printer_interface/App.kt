package com.taviak.printer_interface

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import com.taviak.printer_interface.data.AppDatabase

class App : Application() {

    companion object {
        private const val PREFS = "com.hlypalo.express_kassa"

        lateinit var sharedPrefs: SharedPreferences
            private set
        lateinit var prefEditor: SharedPreferences.Editor
            private set
        lateinit var db: AppDatabase
            private set
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate() {
        super.onCreate()

        sharedPrefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        prefEditor = sharedPrefs.edit()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "Bluetooth Thermal Printer Interface"
        ).fallbackToDestructiveMigration().build()
    }
}