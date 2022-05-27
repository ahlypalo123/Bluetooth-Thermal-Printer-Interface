package com.taviak.printer_interface.data.converter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.taviak.printer_interface.data.model.ListData
import java.lang.reflect.Type

class ListDataDeserializer : JsonDeserializer<ListData> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ListData? {
        return context?.deserialize(json, (object : TypeToken<ListData>() {}).type)
    }
}