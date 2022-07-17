package com.taviak.printer_interface.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taviak.printer_interface.data.converter.ListOfStringConverter
import com.taviak.printer_interface.data.converter.ReceiptTemplateDataConverter
import com.taviak.printer_interface.data.dao.ReceiptTemplateDao
import com.taviak.printer_interface.data.dao.VariableDao
import com.taviak.printer_interface.data.model.ReceiptTemplate
import com.taviak.printer_interface.data.model.Variable
import com.taviak.printer_interface.data.model.VariableTemplateCrossRef

@Database(entities = [ ReceiptTemplate::class, Variable::class, VariableTemplateCrossRef::class ], version = 4)
@TypeConverters(ReceiptTemplateDataConverter::class, ListOfStringConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun receiptTemplateDao() : ReceiptTemplateDao
    abstract fun variableDao() : VariableDao

}