package com.taviak.printer_interface.data.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.taviak.printer_interface.data.model.ListData
import java.lang.reflect.Type

class ListDataSerializer : JsonSerializer<ListData> {
    override fun serialize(
        src: ListData?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        src ?: return null
        return Gson().toJsonTree(src)
    }
}