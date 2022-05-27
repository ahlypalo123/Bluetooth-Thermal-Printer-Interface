package com.taviak.printer_interface.data.converter

import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.data.model.ReceiptElement
import com.taviak.printer_interface.data.model.ReceiptTemplateData

class ReceiptTemplateDataConverter {

    private val gson = GsonBuilder()
        .registerTypeAdapter(ReceiptElement::class.java, ReceiptElementSerializer())
        .registerTypeAdapter(ReceiptElement::class.java, ReceiptElementDeserializer())
        .create()
    private val typeToken = (object : TypeToken<ReceiptTemplateData>() {}).type

    @TypeConverter
    fun fromReceiptTemplateData(data: ReceiptTemplateData) : String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun toReceiptTemplateData(data: String) : ReceiptTemplateData {
        return gson.fromJson(data, typeToken)
    }

}