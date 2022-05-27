package com.taviak.printer_interface.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListOfStringConverter {

    private val gson = Gson()
    private val typeToken = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromListOfString(data: List<String>) : String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun toListOfString(data: String) : List<String> {
        return gson.fromJson(data, typeToken)
    }

}